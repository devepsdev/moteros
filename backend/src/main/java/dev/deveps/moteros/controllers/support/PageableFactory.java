package dev.deveps.moteros.controllers.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Construye el {@link Pageable} a partir de los campos de paginacion de los *SearchDTO / *FilterDTO. */
public final class PageableFactory {

    private PageableFactory() {
    }

    /** Pageable sin ordenacion (para queries cuyo SELECT no permite ORDER BY por alias). */
    public static Pageable of(int page, int size) {
        return PageRequest.of(Math.max(page, 0), size <= 0 ? 10 : size);
    }

    public static Pageable of(int page, int size, String sortBy, String sortDir, String sortByPorDefecto) {
        int p = Math.max(page, 0);
        int s = size <= 0 ? 10 : size;
        String campo = (sortBy == null || sortBy.isBlank()) ? sortByPorDefecto : sortBy;
        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(campo).descending()
                : Sort.by(campo).ascending();
        return PageRequest.of(p, s, sort);
    }
}
