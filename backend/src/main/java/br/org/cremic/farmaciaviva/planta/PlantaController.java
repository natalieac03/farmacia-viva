package br.org.cremic.farmaciaviva.planta;

import br.org.cremic.farmaciaviva.shared.dto.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/plantas")
public class PlantaController {

    private final PlantaService service;

    public PlantaController(PlantaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PlantaResponse> criar(@Valid @RequestBody PlantaRequest request) {
        PlantaResponse response = service.criar(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<PlantaResumoResponse>> listar(
        @RequestParam(name = "busca", required = false) String busca,
        @RequestParam(name = "familiaBotanica", required = false) String familiaBotanica,
        @RequestParam(name = "ativo", required = false) Boolean ativo,
        @PageableDefault(size = 20, sort = "nomeCientifico", direction = Sort.Direction.ASC)
        Pageable pageable
    ) {
        PlantaFiltro filtro = new PlantaFiltro(busca, familiaBotanica, ativo);
        return ResponseEntity.ok(service.listar(filtro, pageable));
    }

    @GetMapping("/opcoes")
    public ResponseEntity<List<PlantaOpcaoResponse>> opcoes() {
        return ResponseEntity.ok(service.opcoes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantaResponse> atualizar(
        @PathVariable UUID id,
        @Valid @RequestBody PlantaRequest request
    ) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @PatchMapping("/{id}/arquivar")
    public ResponseEntity<PlantaResponse> arquivar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.arquivar(id));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<PlantaResponse> ativar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.ativar(id));
    }

    @PostMapping("/{id}/similares")
    public ResponseEntity<PlantaResponse> vincularSimilar(
        @PathVariable UUID id,
        @Valid @RequestBody VincularSimilarRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.vincularSimilar(id, request));
    }

    @DeleteMapping("/{id}/similares/{similarId}")
    public ResponseEntity<Void> desvincularSimilar(
        @PathVariable UUID id,
        @PathVariable UUID similarId
    ) {
        service.desvincularSimilar(id, similarId);
        return ResponseEntity.noContent().build();
    }
}
