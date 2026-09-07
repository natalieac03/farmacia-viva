package br.org.cremic.farmaciaviva.unidademedida;

import br.org.cremic.farmaciaviva.shared.dto.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/unidades-medida")
public class UnidadeMedidaController {

    private final UnidadeMedidaService service;

    public UnidadeMedidaController(UnidadeMedidaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UnidadeMedidaResponse> criar(
        @Valid @RequestBody UnidadeMedidaRequest request
    ) {
        UnidadeMedidaResponse response = service.criar(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<UnidadeMedidaResponse>> listar(
        @RequestParam(name = "ativo", required = false) Boolean ativo,
        @PageableDefault(size = 20, sort = "codigo", direction = Sort.Direction.ASC)
        Pageable pageable
    ) {
        return ResponseEntity.ok(service.listar(ativo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadeMedidaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnidadeMedidaResponse> atualizar(
        @PathVariable UUID id,
        @Valid @RequestBody UnidadeMedidaUpdateRequest request
    ) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @PatchMapping("/{id}/arquivar")
    public ResponseEntity<UnidadeMedidaResponse> arquivar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.arquivar(id));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<UnidadeMedidaResponse> ativar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.ativar(id));
    }
}
