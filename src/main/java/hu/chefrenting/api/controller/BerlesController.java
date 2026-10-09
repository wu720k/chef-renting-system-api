package hu.chefrenting.api.controller;

import hu.chefrenting.api.dto.BerlesRequest;
import hu.chefrenting.api.entity.Berles;
import hu.chefrenting.api.service.BerlesService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/berlesek")
public class BerlesController {
    private final BerlesService service;

    public BerlesController(BerlesService service) {
        this.service = service;
    }

    @GetMapping
    public List<Berles> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Berles getById(@PathVariable int id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<Berles> create(@Valid @RequestBody BerlesRequest request) {
        Berles created = service.create(request);
        return ResponseEntity.created(URI.create("/api/berlesek/" + created.getId())).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
