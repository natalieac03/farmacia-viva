package br.org.cremic.farmaciaviva.planta;

import br.org.cremic.farmaciaviva.shared.dto.PageResponse;
import br.org.cremic.farmaciaviva.shared.exception.BusinessRuleException;
import br.org.cremic.farmaciaviva.shared.exception.DuplicateResourceException;
import br.org.cremic.farmaciaviva.shared.exception.ResourceNotFoundException;
import br.org.cremic.farmaciaviva.shared.texto.NormalizadorTexto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlantaService {

    private final PlantaRepository repository;
    private final PlantaSimilarRepository similarRepository;
    private final PlantaMapper mapper;

    public PlantaService(
        PlantaRepository repository,
        PlantaSimilarRepository similarRepository,
        PlantaMapper mapper
    ) {
        this.repository = repository;
        this.similarRepository = similarRepository;
        this.mapper = mapper;
    }

    @Transactional
    public PlantaResponse criar(PlantaRequest request) {
        String nomeCientifico = request.nomeCientifico().trim();
        String normalizado = NormalizadorTexto.normalizar(nomeCientifico);
        garantirNomeCientificoDisponivel(normalizado, null);

        Planta planta = new Planta();
        aplicarDados(planta, request, nomeCientifico, normalizado);

        // saveAndFlush: as datas de auditoria ja vem preenchidas no DTO e uma
        // eventual violacao do indice unico aparece nesta chamada, nao no commit.
        Planta salva = repository.saveAndFlush(planta);
        return mapper.toResponse(salva, List.of());
    }

    @Transactional
    public PlantaResponse atualizar(UUID id, PlantaRequest request) {
        Planta planta = obterOuFalhar(id);
        String nomeCientifico = request.nomeCientifico().trim();
        String normalizado = NormalizadorTexto.normalizar(nomeCientifico);
        garantirNomeCientificoDisponivel(normalizado, id);

        // As listas de filhos sao substituidas por inteiro. O flush entre o
        // clear e a reinsercao e necessario: o Hibernate executa INSERTs antes
        // de DELETEs dentro de um mesmo flush, e um nome popular mantido na
        // edicao violaria o indice unico (planta_id, nome_normalizado) antes
        // de a linha antiga ser removida pelo orphanRemoval.
        planta.getNomesPopulares().clear();
        planta.getReferencias().clear();
        repository.flush();

        aplicarDados(planta, request, nomeCientifico, normalizado);
        repository.saveAndFlush(planta);
        return montarFicha(id);
    }

    @Transactional(readOnly = true)
    public PlantaResponse buscarPorId(UUID id) {
        return montarFicha(id);
    }

    /**
     * Listagem paginada em tres consultas por pagina, independente do tamanho:
     *   1. a pagina de plantas, com filtros, ordenacao e paginacao no SQL;
     *   2. os nomes populares dessas plantas, em um unico fetch join por id IN;
     *   3. a contagem de similares dessas plantas, em um unico GROUP BY.
     * Ver PlantaRepository.carregarNomesPopulares para o porque de nao usar
     * fetch join direto na consulta paginada.
     */
    @Transactional(readOnly = true)
    public PageResponse<PlantaResumoResponse> listar(PlantaFiltro filtro, Pageable pageable) {
        Page<Planta> pagina = repository.findAll(PlantaSpecifications.comFiltro(filtro), pageable);

        List<UUID> ids = pagina.getContent().stream().map(Planta::getId).toList();
        Map<UUID, Long> similaresPorPlanta = new HashMap<>();
        if (!ids.isEmpty()) {
            repository.carregarNomesPopulares(ids);
            similarRepository.contarPorPlanta(ids)
                .forEach(c -> similaresPorPlanta.put(c.plantaId(), c.quantidade()));
        }

        return PageResponse.from(pagina.map(planta ->
            mapper.toResumo(planta, similaresPorPlanta.getOrDefault(planta.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public List<PlantaOpcaoResponse> opcoes() {
        return repository.listarAtivasComNomesPopulares().stream()
            .map(mapper::toOpcao)
            .toList();
    }

    @Transactional
    public PlantaResponse arquivar(UUID id) {
        Planta planta = obterOuFalhar(id);
        planta.setAtivo(false);
        repository.saveAndFlush(planta);
        // Os vinculos de similaridade sao preservados de proposito: a planta
        // some da listagem ativa mas continua aparecendo como similar das
        // outras, marcada como arquivada.
        return montarFicha(id);
    }

    @Transactional
    public PlantaResponse ativar(UUID id) {
        Planta planta = obterOuFalhar(id);
        planta.setAtivo(true);
        repository.saveAndFlush(planta);
        return montarFicha(id);
    }

    /**
     * Cria o vinculo nos dois sentidos, na mesma transacao (ver PlantaSimilar).
     */
    @Transactional
    public PlantaResponse vincularSimilar(UUID id, VincularSimilarRequest request) {
        UUID similarId = request.plantaSimilarId();
        if (id.equals(similarId)) {
            throw new BusinessRuleException(
                "Uma planta nao pode ser vinculada como similar dela mesma");
        }

        Planta planta = obterOuFalhar(id);
        Planta similar = obterOuFalhar(similarId);

        if (!planta.isAtivo()) {
            throw new BusinessRuleException(
                "Planta arquivada nao pode receber vinculo de similaridade: "
                    + planta.getNomeCientifico());
        }
        if (!similar.isAtivo()) {
            throw new BusinessRuleException(
                "Nao e possivel vincular uma planta arquivada como similar: "
                    + similar.getNomeCientifico());
        }
        if (similarRepository.existsByPlantaIdAndPlantaSimilarId(id, similarId)) {
            throw new DuplicateResourceException(
                "As plantas ja estao vinculadas como similares: "
                    + planta.getNomeCientifico() + " e " + similar.getNomeCientifico());
        }

        String observacao = NormalizadorTexto.emBrancoParaNulo(request.observacao());
        similarRepository.save(novoVinculo(planta, similar, observacao));
        similarRepository.save(novoVinculo(similar, planta, observacao));
        similarRepository.flush();

        return montarFicha(id);
    }

    /** Remove o vinculo nos dois sentidos. */
    @Transactional
    public void desvincularSimilar(UUID id, UUID similarId) {
        obterOuFalhar(id);
        long removidos = similarRepository.deleteByPlantaIdAndPlantaSimilarId(id, similarId)
            + similarRepository.deleteByPlantaIdAndPlantaSimilarId(similarId, id);
        if (removidos == 0) {
            throw new ResourceNotFoundException(
                "Vinculo de similaridade nao encontrado entre " + id + " e " + similarId);
        }
        similarRepository.flush();
    }

    private PlantaResponse montarFicha(UUID id) {
        Planta planta = repository.buscarComNomesPopulares(id)
            .orElseThrow(() -> new ResourceNotFoundException("Planta nao encontrada: " + id));
        // Mesma instancia gerenciada; esta chamada apenas inicializa as
        // referencias (duas bags nao podem ser buscadas no mesmo fetch join).
        repository.buscarComReferencias(id);

        List<PlantaResponse.SimilarResponse> similares = similarRepository.listarPorPlanta(id)
            .stream()
            .map(mapper::toSimilarResponse)
            .toList();

        return mapper.toResponse(planta, similares);
    }

    private Planta obterOuFalhar(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Planta nao encontrada: " + id));
    }

    private void garantirNomeCientificoDisponivel(String normalizado, UUID idAtual) {
        Optional<Planta> existente = idAtual == null
            ? repository.findByNomeCientificoNormalizado(normalizado)
            : repository.findByNomeCientificoNormalizadoAndIdNot(normalizado, idAtual);
        existente.ifPresent(outra -> {
            throw new DuplicateResourceException(
                "Ja existe outra planta cadastrada com este nome cientifico: "
                    + outra.getNomeCientifico() + " (id " + outra.getId() + ")");
        });
    }

    private void aplicarDados(
        Planta planta,
        PlantaRequest request,
        String nomeCientifico,
        String normalizado
    ) {
        planta.setNomeCientifico(nomeCientifico);
        planta.setNomeCientificoNormalizado(normalizado);
        planta.setFamiliaBotanica(NormalizadorTexto.emBrancoParaNulo(request.familiaBotanica()));
        planta.setCultivo(NormalizadorTexto.emBrancoParaNulo(request.cultivo()));
        planta.setIndicacaoUso(NormalizadorTexto.emBrancoParaNulo(request.indicacaoUso()));
        planta.setObservacoes(NormalizadorTexto.emBrancoParaNulo(request.observacoes()));

        montarNomesPopulares(request.nomesPopulares()).forEach(planta::adicionarNomePopular);
        montarReferencias(request.referencias()).forEach(planta::adicionarReferencia);
    }

    private List<PlantaNomePopular> montarNomesPopulares(
        List<PlantaRequest.NomePopularRequest> nomes
    ) {
        List<PlantaNomePopular> resultado = new ArrayList<>();
        if (nomes == null) {
            return resultado;
        }
        Set<String> vistos = new HashSet<>();
        boolean principalDefinido = false;
        int ordem = 0;
        for (PlantaRequest.NomePopularRequest item : nomes) {
            String nome = item.nome().trim();
            String normalizado = NormalizadorTexto.normalizar(nome);
            if (!vistos.add(normalizado)) {
                throw new BusinessRuleException(
                    "Nome popular repetido na mesma planta: " + nome);
            }
            boolean principal = Boolean.TRUE.equals(item.principal());
            if (principal) {
                if (principalDefinido) {
                    throw new BusinessRuleException(
                        "Apenas um nome popular pode ser marcado como principal");
                }
                principalDefinido = true;
            }
            PlantaNomePopular entidade = new PlantaNomePopular();
            entidade.setNome(nome);
            entidade.setNomeNormalizado(normalizado);
            entidade.setPrincipal(principal);
            entidade.setOrdem(ordem++);
            resultado.add(entidade);
        }
        return resultado;
    }

    private List<PlantaReferencia> montarReferencias(
        List<PlantaRequest.ReferenciaRequest> referencias
    ) {
        List<PlantaReferencia> resultado = new ArrayList<>();
        if (referencias == null) {
            return resultado;
        }
        int ordem = 0;
        for (PlantaRequest.ReferenciaRequest item : referencias) {
            String titulo = NormalizadorTexto.emBrancoParaNulo(item.titulo());
            String textoLivre = NormalizadorTexto.emBrancoParaNulo(item.textoLivre());
            if (titulo == null && textoLivre == null) {
                throw new BusinessRuleException(
                    "Referencia " + (ordem + 1) + " precisa de titulo ou texto livre");
            }
            PlantaReferencia entidade = new PlantaReferencia();
            entidade.setTipo(item.tipo());
            entidade.setAutor(NormalizadorTexto.emBrancoParaNulo(item.autor()));
            entidade.setTitulo(titulo);
            entidade.setAno(item.ano());
            entidade.setLink(NormalizadorTexto.emBrancoParaNulo(item.link()));
            entidade.setTextoLivre(textoLivre);
            entidade.setOrdem(ordem++);
            resultado.add(entidade);
        }
        return resultado;
    }

    private PlantaSimilar novoVinculo(Planta origem, Planta destino, String observacao) {
        PlantaSimilar vinculo = new PlantaSimilar();
        vinculo.setPlanta(origem);
        vinculo.setPlantaSimilar(destino);
        vinculo.setObservacao(observacao);
        return vinculo;
    }
}
