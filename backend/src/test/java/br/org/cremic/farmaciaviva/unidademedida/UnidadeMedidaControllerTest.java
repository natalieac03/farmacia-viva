package br.org.cremic.farmaciaviva.unidademedida;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.org.cremic.farmaciaviva.shared.dto.PageResponse;
import br.org.cremic.farmaciaviva.shared.exception.DuplicateResourceException;
import br.org.cremic.farmaciaviva.shared.exception.GlobalExceptionHandler;
import br.org.cremic.farmaciaviva.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UnidadeMedidaControllerTest {

    @Mock
    private UnidadeMedidaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UnidadeMedidaController controller = new UnidadeMedidaController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(Clock.systemUTC()))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    private UnidadeMedidaResponse response(UUID id, String codigo, boolean ativo) {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        return new UnidadeMedidaResponse(
            id, codigo, "Grama", Dimensao.MASSA,
            new BigDecimal("1.000000"), true, 3, ativo, agora, agora);
    }

    @Test
    @DisplayName("POST deve retornar 201 com Location e o corpo criado")
    void postDeveRetornar201() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.criar(any(UnidadeMedidaRequest.class)))
            .thenReturn(response(id, "G", true));

        String body = """
            {
              "codigo": "g",
              "nome": "Grama",
              "dimensao": "MASSA",
              "fatorParaBase": 1.0,
              "unidadeBase": true,
              "casasDecimais": 3
            }
            """;

        mockMvc.perform(post("/api/v1/unidades-medida")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location",
                org.hamcrest.Matchers.endsWith("/api/v1/unidades-medida/" + id)))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.codigo").value("G"))
            .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("POST invalido deve retornar 400 com fieldErrors e nao chamar o service")
    void postInvalidoDeveRetornar400() throws Exception {
        String body = """
            {
              "codigo": "",
              "nome": "",
              "dimensao": "MASSA",
              "fatorParaBase": -1,
              "unidadeBase": null,
              "casasDecimais": 9
            }
            """;

        mockMvc.perform(post("/api/v1/unidades-medida")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.fieldErrors").isArray())
            .andExpect(jsonPath("$.fieldErrors").isNotEmpty());

        verify(service, never()).criar(any());
    }

    @Test
    @DisplayName("POST com codigo duplicado deve retornar 409")
    void postDuplicadoDeveRetornar409() throws Exception {
        when(service.criar(any(UnidadeMedidaRequest.class)))
            .thenThrow(new DuplicateResourceException(
                "Ja existe uma unidade de medida com o codigo G"));

        String body = """
            {
              "codigo": "G",
              "nome": "Grama",
              "dimensao": "MASSA",
              "fatorParaBase": 1.0,
              "unidadeBase": true,
              "casasDecimais": 3
            }
            """;

        mockMvc.perform(post("/api/v1/unidades-medida")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET deve retornar 200 com a pagina e repassar o filtro ativo")
    void getListaDeveRetornar200ComFiltro() throws Exception {
        PageResponse<UnidadeMedidaResponse> pagina = new PageResponse<>(
            List.of(response(UUID.randomUUID(), "G", true)),
            0, 20, 1, 1, true, true);
        when(service.listar(eq(Boolean.TRUE), any(Pageable.class))).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/unidades-medida").param("ativo", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].codigo").value("G"))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(service).listar(eq(Boolean.TRUE), any(Pageable.class));
    }

    @Test
    @DisplayName("GET por id inexistente deve retornar 404")
    void getPorIdInexistenteDeveRetornar404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id))
            .thenThrow(new ResourceNotFoundException(
                "Unidade de medida nao encontrada: " + id));

        mockMvc.perform(get("/api/v1/unidades-medida/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT deve retornar 200 com o corpo atualizado")
    void putDeveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.atualizar(eq(id), any(UnidadeMedidaUpdateRequest.class)))
            .thenReturn(response(id, "KG", true));

        String body = """
            {
              "codigo": "kg",
              "nome": "Quilograma",
              "dimensao": "MASSA",
              "fatorParaBase": 1000.0,
              "unidadeBase": false,
              "casasDecimais": 3
            }
            """;

        mockMvc.perform(put("/api/v1/unidades-medida/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo").value("KG"));
    }

    @Test
    @DisplayName("PATCH arquivar deve retornar 200 com ativo false")
    void patchArquivarDeveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.arquivar(id)).thenReturn(response(id, "G", false));

        mockMvc.perform(patch("/api/v1/unidades-medida/{id}/arquivar", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    @DisplayName("PATCH ativar deve retornar 200 com ativo true")
    void patchAtivarDeveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.ativar(id)).thenReturn(response(id, "G", true));

        mockMvc.perform(patch("/api/v1/unidades-medida/{id}/ativar", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ativo").value(true));
    }
}
