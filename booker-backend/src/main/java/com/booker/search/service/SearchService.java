package com.booker.search.service;

import com.booker.search.dto.BusinessSearchResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SearchService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<BusinessSearchResult> searchBusinesses(
            Double lat, Double lng, double radiusKm,
            Long categoryId, String query,
            Pageable pageable) {

        boolean hasGeo = lat != null && lng != null;

        // ── Dynamic WHERE clause ───────────────────────────────────
        StringBuilder where = new StringBuilder(
                "WHERE b.status = 'ACTIVE' AND br.status = 'ACTIVE'");

        if (categoryId != null) {
            where.append(" AND b.category_id = :categoryId");
        }
        if (query != null && !query.isBlank()) {
            where.append(" AND (b.name ILIKE :queryLike OR b.description ILIKE :queryLike)");
        }
        if (hasGeo) {
            where.append(
                    " AND ST_DWithin(br.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusM)");
        }

        // ── Distance expression ────────────────────────────────────
        String distanceSel = hasGeo
                ? "ST_Distance(br.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)"
                : "NULL::double precision";

        String orderBy = hasGeo
                ? "ORDER BY " + distanceSel + " ASC NULLS LAST"
                : "ORDER BY b.id DESC";

        // ── Data query ─────────────────────────────────────────────
        String dataSql = """
                SELECT b.id, b.name, b.description, b.logo_url,
                       c.id, c.name, c.label,
                       br.city, br.address, br.timezone,
                       """ + distanceSel + """

                FROM businesses b
                JOIN business_categories c ON c.id = b.category_id
                JOIN branches br ON br.business_id = b.id
                """ + where + " " + orderBy;

        // ── Count query ────────────────────────────────────────────
        String countSql = """
                SELECT COUNT(*)
                FROM businesses b
                JOIN branches br ON br.business_id = b.id
                """ + where;

        Query dataQ  = entityManager.createNativeQuery(dataSql);
        Query countQ = entityManager.createNativeQuery(countSql);

        bindParams(dataQ, countQ, hasGeo, lat, lng, radiusKm, categoryId, query);

        dataQ.setFirstResult((int) pageable.getOffset());
        dataQ.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQ.getResultList();
        long total = ((Number) countQ.getSingleResult()).longValue();

        List<BusinessSearchResult> results = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            results.add(new BusinessSearchResult(
                    ((Number) r[0]).longValue(),        // id
                    (String)  r[1],                     // name
                    (String)  r[2],                     // description
                    (String)  r[3],                     // logoUrl
                    ((Number) r[4]).longValue(),        // categoryId
                    (String)  r[5],                     // categoryName
                    (String)  r[6],                     // categoryLabel
                    (String)  r[7],                     // city
                    (String)  r[8],                     // address
                    (String)  r[9],                     // timezone
                    r[10] != null ? ((Number) r[10]).doubleValue() : null  // distanceMeters
            ));
        }

        return new PageImpl<>(results, pageable, total);
    }

    private void bindParams(Query dataQ, Query countQ,
                            boolean hasGeo, Double lat, Double lng, double radiusKm,
                            Long categoryId, String query) {
        if (categoryId != null) {
            dataQ.setParameter("categoryId", categoryId);
            countQ.setParameter("categoryId", categoryId);
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query + "%";
            dataQ.setParameter("queryLike", like);
            countQ.setParameter("queryLike", like);
        }
        if (hasGeo) {
            double radiusM = radiusKm * 1000.0;
            dataQ.setParameter("lat", lat);
            dataQ.setParameter("lng", lng);
            dataQ.setParameter("radiusM", radiusM);
            countQ.setParameter("lat", lat);
            countQ.setParameter("lng", lng);
            countQ.setParameter("radiusM", radiusM);
        }
    }
}
