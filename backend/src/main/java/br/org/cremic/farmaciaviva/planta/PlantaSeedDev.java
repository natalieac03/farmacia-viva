package br.org.cremic.farmaciaviva.planta;

import br.org.cremic.farmaciaviva.planta.PlantaRequest.NomePopularRequest;
import br.org.cremic.farmaciaviva.planta.PlantaRequest.ReferenciaRequest;
import br.org.cremic.farmaciaviva.shared.texto.NormalizadorTexto;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seed de desenvolvimento do repositorio de plantas. So roda no profile dev.
 *
 * Idempotente: cada planta e procurada pelo nome cientifico NORMALIZADO (a
 * identidade real da ficha), nunca por UUID fixo, e so e criada se nao
 * existir; o vinculo de similaridade so e criado se ainda nao houver. Rodar
 * duas vezes nao duplica nada.
 *
 * As plantas conversam com os dados demonstrativos do frontend (Guaco e
 * Espinheira-santa aparecem nas telas de estoque). Passa pelo PlantaService
 * para que as mesmas regras de negocio valham para o seed.
 */
@Component
@Profile("dev")
public class PlantaSeedDev implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlantaSeedDev.class);

    private final PlantaService service;
    private final PlantaRepository repository;
    private final PlantaSimilarRepository similarRepository;

    public PlantaSeedDev(
        PlantaService service,
        PlantaRepository repository,
        PlantaSimilarRepository similarRepository
    ) {
        this.service = service;
        this.repository = repository;
        this.similarRepository = similarRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        ReferenciaRequest renisus = new ReferenciaRequest(
            TipoReferencia.LEGISLACAO,
            "Brasil. Ministério da Saúde",
            "RENISUS – Relação Nacional de Plantas Medicinais de Interesse ao SUS",
            2009, null, null);
        ReferenciaRequest formulario = new ReferenciaRequest(
            TipoReferencia.LIVRO,
            "Agência Nacional de Vigilância Sanitária (Anvisa)",
            "Formulário de Fitoterápicos da Farmacopeia Brasileira, 2ª edição",
            2021,
            "https://www.gov.br/anvisa/pt-br/assuntos/farmacopeia/formulario-fitoterapico",
            null);
        ReferenciaRequest lorenzi = new ReferenciaRequest(
            TipoReferencia.LIVRO,
            "Lorenzi H, Matos FJA",
            "Plantas Medicinais no Brasil: nativas e exóticas, 2ª edição",
            2008, null, null);

        UUID mikaniaGlomerata = garantir(new PlantaRequest(
            "Mikania glomerata Spreng.",
            "Asteraceae",
            "Trepadeira perene, propagada por estacas. Prefere solos férteis e úmidos, "
                + "de meia-sombra a pleno sol; precisa de suporte para se apoiar. As folhas "
                + "podem ser colhidas a partir do primeiro ano.",
            "Expectorante e broncodilatador, tradicionalmente usado em tosses e afecções "
                + "das vias respiratórias (xarope de guaco). Uso reconhecido na RENISUS e no "
                + "Formulário de Fitoterápicos da Farmacopeia Brasileira.",
            "Espécie de referência para o guaco nas farmácias vivas.",
            List.of(
                new NomePopularRequest("Guaco", true),
                new NomePopularRequest("Guaco-liso", false),
                new NomePopularRequest("Cipó-caatinga", false)),
            List.of(
                renisus,
                formulario,
                new ReferenciaRequest(
                    TipoReferencia.ARTIGO,
                    "Soares de Moura R, et al.",
                    "Bronchodilator activity of Mikania glomerata Sprengel on human bronchi "
                        + "and guinea-pig trachea",
                    2002, null,
                    "Journal of Pharmacy and Pharmacology, 54(2):249-256."),
                new ReferenciaRequest(
                    TipoReferencia.OUTRO, null, null, null, null,
                    "Anotação de campo do PET-Saúde: canteiro da Farmácia Viva junto ao "
                        + "muro leste, mudas de estaca plantadas em 2025."))));

        UUID mikaniaLaevigata = garantir(new PlantaRequest(
            "Mikania laevigata Sch.Bip. ex Baker",
            "Asteraceae",
            "Trepadeira perene semelhante a M. glomerata, também propagada por estacas. "
                + "Aroma marcante de cumarina nas folhas, mais intenso após a secagem.",
            "Mesmo uso tradicional de Mikania glomerata (expectorante). É a espécie com "
                + "maior teor de cumarina, responsável pelo aroma característico.",
            "Frequentemente comercializada e cultivada como guaco sem distinção de espécie.",
            List.of(
                new NomePopularRequest("Guaco", true),
                new NomePopularRequest("Guaco-cheiroso", false),
                new NomePopularRequest("Guaco-de-cheiro", false)),
            List.of(lorenzi)));

        UUID maytenus = garantir(new PlantaRequest(
            "Maytenus ilicifolia Mart. ex Reissek",
            "Celastraceae",
            "Arbusto ou árvore pequena de crescimento lento, propagada por sementes. Tolera "
                + "sombra parcial e prefere solos bem drenados. Colhem-se folhas maduras.",
            "Auxiliar no tratamento de gastrite, úlcera gástrica e dispepsia. Presente na "
                + "RENISUS e no Formulário de Fitoterápicos.",
            null,
            List.of(
                new NomePopularRequest("Espinheira-santa", true),
                new NomePopularRequest("Cancorosa", false),
                new NomePopularRequest("Espinheira-divina", false)),
            List.of(
                renisus,
                new ReferenciaRequest(
                    TipoReferencia.SITE,
                    "Anvisa",
                    "Formulário de Fitoterápicos da Farmacopeia Brasileira (página oficial)",
                    null,
                    "https://www.gov.br/anvisa/pt-br/assuntos/farmacopeia/formulario-fitoterapico",
                    null))));

        garantir(new PlantaRequest(
            "Lippia alba (Mill.) N.E.Br. ex Britton & P.Wilson",
            "Verbenaceae",
            "Arbusto aromático rústico, propagado por estacas. Pleno sol e solos leves; "
                + "podas frequentes estimulam a brotação.",
            "Ansiolítico leve e sedativo suave; também usado em cólicas e dispepsia "
                + "(chá das folhas).",
            "Compartilha o nome popular erva-cidreira com Melissa officinalis e com "
                + "Cymbopogon citratus (capim-limão). Confirmar a espécie antes de dispensar.",
            List.of(
                new NomePopularRequest("Erva-cidreira", true),
                new NomePopularRequest("Erva-cidreira-de-arbusto", false),
                new NomePopularRequest("Falsa-melissa", false),
                new NomePopularRequest("Salva-limão", false)),
            List.of(renisus, formulario)));

        garantirVinculo(mikaniaGlomerata, mikaniaLaevigata,
            "Ambas são chamadas de guaco e usadas como expectorante; M. laevigata tem "
                + "maior teor de cumarina.");

        log.info("Seed de plantas concluido: {} plantas no repositorio (Maytenus id {})",
            repository.count(), maytenus);
    }

    private UUID garantir(PlantaRequest request) {
        String normalizado = NormalizadorTexto.normalizar(request.nomeCientifico());
        return repository.findByNomeCientificoNormalizado(normalizado)
            .map(Planta::getId)
            .orElseGet(() -> {
                UUID id = service.criar(request).id();
                log.info("Seed: planta criada '{}' ({})", request.nomeCientifico(), id);
                return id;
            });
    }

    private void garantirVinculo(UUID plantaId, UUID similarId, String observacao) {
        if (similarRepository.existsByPlantaIdAndPlantaSimilarId(plantaId, similarId)) {
            return;
        }
        service.vincularSimilar(plantaId, new VincularSimilarRequest(similarId, observacao));
        log.info("Seed: vinculo de similaridade criado entre {} e {}", plantaId, similarId);
    }
}
