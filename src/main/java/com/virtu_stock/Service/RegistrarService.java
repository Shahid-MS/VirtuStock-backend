package com.virtu_stock.Service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.virtu_stock.Models.Registrar;
import com.virtu_stock.Repository.RegistrarRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrarService {

    private final RegistrarRepository registrarRepository;

    public Registrar findOrCreate(
            String name,
            String slug,
            String url) {

        if (slug == null || slug.isBlank()) {
            return null;
        }

        return registrarRepository.findBySlug(slug)
                .map(registrar -> {

                    boolean changed = false;

                    if (!Objects.equals(registrar.getName(), name)) {
                        registrar.setName(name);
                        changed = true;
                    }

                    if (!Objects.equals(registrar.getUrl(), url)) {
                        registrar.setUrl(url);
                        changed = true;
                    }

                    return changed
                            ? registrarRepository.save(registrar)
                            : registrar;
                })
                .orElseGet(() -> {
                    Registrar registrar = new Registrar();
                    registrar.setName(name);
                    registrar.setSlug(slug);
                    registrar.setUrl(url);

                    return registrarRepository.save(registrar);
                });
    }
}
