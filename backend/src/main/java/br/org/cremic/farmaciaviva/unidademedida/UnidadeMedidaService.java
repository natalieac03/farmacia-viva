package br.org.cremic.farmaciaviva.unidademedida;

import br.org.cremic.farmaciaviva.shared.dto.PageResponse;
import br.org.cremic.farmaciaviva.shared.exception.BusinessRuleException;
import br.org.cremic.farmaciaviva.shared.exception.DuplicateResourceException;
import br.org.cremic.farmaciaviva.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadeMedidaService {

    /** Escala do fator de conversao, identica a definida na coluna NUMERIC(19,6). */
    static final int ESCALA_FATOR_PARA_BASE = 6;

    static final int CASAS_DECIMAIS_MINIMO = 0;
    static final int CASAS_DECIMAIS_MAXIMO = 6;

    private final UnidadeMedidaRepository repository;
    private final UnidadeMedidaMapper mapper;

    public UnidadeMedidaService(
        UnidadeMedidaRepository repository,
        UnidadeMedidaMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public UnidadeMedidaResponse criar(UnidadeMedidaRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        BigDecimal fator = normalizarFator(request.fatorParaBase());
        validarCasasDecimais(request.casasDecimais());
        garantirCodigoDisponivelNaCriacao(codigo);

        UnidadeMedida entidade = mapper.toEntity(request);
        entidade.setCodigo(codigo);
        entidade.setFatorParaBase(fator);

        // saveAndFlush garante que as datas de auditoria ja estejam preenchidas
        // quando a entidade for convertida em DTO e que uma eventual violacao do
        // indice unico ocorra dentro desta chamada, e nao apenas no commit.
        UnidadeMedida salva = repository.saveAndFlush(entidade);
        return mapper.toResponse(salva);
    }

    @Transactional(readOnly = true)
    public PageResponse<UnidadeMedidaResponse> listar(Boolean ativo, Pageable pageable) {
        Page<UnidadeMedida> pagina = (ativo == null)
            ? repository.findAll(pageable)
            : repository.findByAtivo(ativo, pageable);
        return PageResponse.from(pagina.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UnidadeMedidaResponse buscarPorId(UUID id) {
        return mapper.toResponse(obterOuFalhar(id));
    }

    @Transactional
    public UnidadeMedidaResponse atualizar(UUID id, UnidadeMedidaUpdateRequest request) {
        UnidadeMedida entidade = obterOuFalhar(id);
        String codigo = normalizarCodigo(request.codigo());
        BigDecimal fator = normalizarFator(request.fatorParaBase());
        validarCasasDecimais(request.casasDecimais());
        garantirCodigoDisponivelNaAtualizacao(codigo, id);

        mapper.updateEntity(request, entidade);
        entidade.setCodigo(codigo);
        entidade.setFatorParaBase(fator);

        UnidadeMedida salva = repository.saveAndFlush(entidade);
        return mapper.toResponse(salva);
    }

    @Transactional
    public UnidadeMedidaResponse arquivar(UUID id) {
        UnidadeMedida entidade = obterOuFalhar(id);
        entidade.setAtivo(false);
        return mapper.toResponse(repository.saveAndFlush(entidade));
    }

    @Transactional
    public UnidadeMedidaResponse ativar(UUID id) {
        UnidadeMedida entidade = obterOuFalhar(id);
        entidade.setAtivo(true);
        return mapper.toResponse(repository.saveAndFlush(entidade));
    }

    private UnidadeMedida obterOuFalhar(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Unidade de medida nao encontrada: " + id));
    }

    private void garantirCodigoDisponivelNaCriacao(String codigo) {
        if (repository.existsByCodigoIgnoreCase(codigo)) {
            throw new DuplicateResourceException(
                "Ja existe uma unidade de medida com o codigo " + codigo);
        }
    }

    private void garantirCodigoDisponivelNaAtualizacao(String codigo, UUID id) {
        if (repository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new DuplicateResourceException(
                "Ja existe uma unidade de medida com o codigo " + codigo);
        }
    }

    private String normalizarCodigo(String codigo) {
        String normalizado = codigo.trim().toUpperCase(Locale.ROOT);
        if (normalizado.isEmpty()) {
            throw new BusinessRuleException("codigo e obrigatorio");
        }
        return normalizado;
    }

    /**
     * Ajusta o fator para a escala da coluna sem arredondamento silencioso: um
     * valor com mais casas decimais do que a coluna suporta e rejeitado em vez
     * de ser truncado pelo banco.
     */
    private BigDecimal normalizarFator(BigDecimal fatorParaBase) {
        if (fatorParaBase.signum() <= 0) {
            throw new BusinessRuleException("fatorParaBase deve ser maior que zero");
        }
        if (fatorParaBase.stripTrailingZeros().scale() > ESCALA_FATOR_PARA_BASE) {
            throw new BusinessRuleException(
                "fatorParaBase deve ter no maximo " + ESCALA_FATOR_PARA_BASE + " casas decimais");
        }
        return fatorParaBase.setScale(ESCALA_FATOR_PARA_BASE, RoundingMode.UNNECESSARY);
    }

    private void validarCasasDecimais(Integer casasDecimais) {
        if (casasDecimais < CASAS_DECIMAIS_MINIMO || casasDecimais > CASAS_DECIMAIS_MAXIMO) {
            throw new BusinessRuleException(
                "casasDecimais deve estar entre " + CASAS_DECIMAIS_MINIMO
                    + " e " + CASAS_DECIMAIS_MAXIMO);
        }
    }
}
