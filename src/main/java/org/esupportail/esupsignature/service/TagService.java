package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Tag;
import org.esupportail.esupsignature.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TagService {

    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    /** Palette de couleurs par défaut attribuée automatiquement aux nouvelles racines */
    private static final String[] DEFAULT_ROOT_COLORS = {
            "#1565C0", "#1B5E20", "#880E4F", "#E65100", "#4A148C",
            "#006064", "#BF360C", "#37474F", "#558B2F", "#0D47A1"
    };

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public Page<Tag> getAllTags(Pageable pageable) {
        return tagRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }


    /** Retourne tous les tags racines (parentTag == null), utilisés comme thèmes/colonnes */
    @Transactional(readOnly = true)
    public List<Tag> getAllRootTags() {
        return tagRepository.findByParentTagIsNull();
    }

    /** Retourne les enfants directs d'un tag */
    @Transactional(readOnly = true)
    public List<Tag> getChildren(Tag parent) {
        return tagRepository.findByParentTag(parent);
    }

    /**
     * Retourne les noms de tous les groupes (tags racines = parentTag == null).
     * Ces groupes définissent les colonnes de la liste des demandes.
     */
    @Transactional(readOnly = true)
    public List<String> getAllGroupNames() {
        return tagRepository.findByParentTagIsNull().stream()
                .sorted(rootOrder())
                .map(Tag::getName)
                .collect(Collectors.toList());
    }

    /** Retourne un map ordonné groupName → couleur pour tous les groupes (racines). */
    @Transactional(readOnly = true)
    public Map<String, String> getAllGroupColors() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        tagRepository.findByParentTagIsNull().stream()
                .sorted(rootOrder())
                .forEach(t -> result.putIfAbsent(t.getName(), t.getColor() != null ? t.getColor() : "#6c757d"));
        return result;
    }

    /**
     * Retourne un map ordonné : Groupe (racine) → liste de ses tags (enfants directs).
     * Utilisé par la page d'administration.
     */
    @Transactional(readOnly = true)
    public Map<Tag, List<Tag>> getGroupsWithTags() {
        LinkedHashMap<Tag, List<Tag>> result = new LinkedHashMap<>();
        tagRepository.findByParentTagIsNull().stream()
                .sorted(rootOrder())
                .forEach(group -> {
                    List<Tag> children = tagRepository.findByParentTag(group).stream()
                            .sorted(Comparator.comparing(Tag::getName, String.CASE_INSENSITIVE_ORDER))
                            .collect(Collectors.toList());
                    result.put(group, children);
                });
        return result;
    }

    /** Comparateur pour les groupes racines : displayOrder d'abord, puis alphabétique. */
    private Comparator<Tag> rootOrder() {
        return Comparator.comparingInt(Tag::getDisplayOrder)
                .thenComparing(Tag::getName, String.CASE_INSENSITIVE_ORDER);
    }


    @Transactional
    public Tag createTag(String name, String color) {
        Tag tag = new Tag(name, color);
        tagRepository.save(tag);
        return tag;
    }

    @Transactional
    public Tag createTag(Tag toAdd) {
        // Recharger le parent depuis la DB pour avoir accès à sa couleur
        if (toAdd.getParentTag() != null && toAdd.getParentTag().getId() != null) {
            tagRepository.findById(toAdd.getParentTag().getId()).ifPresent(toAdd::setParentTag);
        }
        if (toAdd.getColor() == null || toAdd.getColor().isBlank()) {
            toAdd.setColor(generateColorForTag(toAdd));
        }
        tagRepository.save(toAdd);
        return toAdd;
    }

    @Transactional
    public void updateTag(Long id, Tag tag) {
        tagRepository.findById(id).ifPresent(t -> {
            t.setName(tag.getName());
            if (tag.getColor() != null && !tag.getColor().isBlank()) {
                t.setColor(tag.getColor());
            }
            if (tag.getDisplayOrder() != null) {
                t.setDisplayOrder(tag.getDisplayOrder());
            }
            if (tag.getParentTag() != null && tag.getParentTag().getId() != null) {
                tagRepository.findById(tag.getParentTag().getId()).ifPresent(t::setParentTag);
            } else {
                t.setParentTag(null);
            }
        });
    }

    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id).orElseThrow();
        tagRepository.delete(tag);
    }

    public Tag getById(Long id) {
        return tagRepository.findById(id).orElseThrow();
    }

    /**
     * Résout ou crée le tag feuille correspondant au chemin hiérarchique.
     * Le chemin est une liste de noms depuis la racine jusqu'à la feuille.
     * Exemple : ["CRM", "Type", "Attestation de formation"]
     *
     * Si un niveau n'existe pas, il est créé automatiquement avec une couleur
     * dérivée de celle du parent (teinte progressivement plus claire via HSL).
     *
     * @param tagPath liste de noms représentant le chemin depuis la racine
     * @return le tag feuille (dernier niveau du chemin)
     */
    @Transactional
    public Tag resolveOrCreate(List<String> tagPath) {
        if (tagPath == null || tagPath.isEmpty()) {
            throw new IllegalArgumentException("tagPath ne peut pas être vide");
        }
        Tag current = null;
        for (int i = 0; i < tagPath.size(); i++) {
            String name = tagPath.get(i).trim();
            Tag found;
            if (i == 0) {
                Optional<Tag> opt = tagRepository.findByNameAndParentTagIsNull(name);
                if (opt.isPresent()) {
                    found = opt.get();
                } else {
                    found = new Tag(name, generateRootColor());
                    tagRepository.save(found);
                    logger.info("Tag racine créé automatiquement : '{}'", name);
                }
            } else {
                Tag parent = current;
                Optional<Tag> opt = tagRepository.findByNameAndParentTag(name, parent);
                if (opt.isPresent()) {
                    found = opt.get();
                } else {
                    String color = darkenOrLighten(parent.getColor(), i);
                    found = new Tag(name, color);
                    found.setParentTag(parent);
                    tagRepository.save(found);
                    logger.info("Tag créé automatiquement : '{}' sous '{}'", name, parent.getName());
                }
            }
            current = found;
        }
        return current;
    }

    /**
     * Retourne le tag racine (ancêtre de niveau 0) d'un tag quelconque.
     */
    public Tag getRootTag(Tag tag) {
        Tag current = tag;
        while (current.getParentTag() != null) {
            current = current.getParentTag();
        }
        return current;
    }

    /**
     * Calcule le niveau de profondeur d'un tag (0 = racine).
     */
    public int getDepth(Tag tag) {
        int depth = 0;
        Tag current = tag;
        while (current.getParentTag() != null) {
            depth++;
            current = current.getParentTag();
        }
        return depth;
    }

    // -------------------------------------------------------------------------
    // Méthodes utilitaires de couleur
    // -------------------------------------------------------------------------

    /**
     * Génère une couleur automatique pour une nouvelle racine.
     * Cycle sur la palette DEFAULT_ROOT_COLORS selon le nombre de racines existantes.
     */
    private String generateRootColor() {
        int existingRoots = tagRepository.findByParentTagIsNull().size();
        return DEFAULT_ROOT_COLORS[existingRoots % DEFAULT_ROOT_COLORS.length];
    }

    /**
     * Génère une couleur pour la création initiale d'un tag (sans parent défini encore).
     */
    private String generateColorForTag(Tag tag) {
        if (tag.getParentTag() == null) {
            return generateRootColor();
        }
        return darkenOrLighten(tag.getParentTag().getColor(), getDepth(tag));
    }

    /**
     * Éclaircit progressivement une couleur hexadécimale selon la profondeur.
     * Chaque niveau ajoute ~15% de luminosité en HSL.
     */
    public static String darkenOrLighten(String hexColor, int depth) {
        try {
            String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
            if (hex.length() != 6) return hexColor;
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            float[] hsl = rgbToHsl(r, g, b);
            hsl[2] = Math.min(0.88f, hsl[2] + depth * 0.15f);
            int[] rgb = hslToRgb(hsl[0], hsl[1], hsl[2]);
            return String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
        } catch (Exception e) {
            logger.warn("Impossible de calculer la couleur dérivée pour '{}': {}", hexColor, e.getMessage());
            return hexColor;
        }
    }

    private static float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float l = (max + min) / 2f;
        float h = 0, s = 0;
        if (max != min) {
            float d = max - min;
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == rf) h = (gf - bf) / d + (gf < bf ? 6 : 0);
            else if (max == gf) h = (bf - rf) / d + 2;
            else h = (rf - gf) / d + 4;
            h /= 6f;
        }
        return new float[]{h, s, l};
    }

    private static int[] hslToRgb(float h, float s, float l) {
        float r, g, b;
        if (s == 0) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hue2rgb(p, q, h + 1f / 3);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1f / 3);
        }
        return new int[]{Math.round(r * 255), Math.round(g * 255), Math.round(b * 255)};
    }

    private static float hue2rgb(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1f / 6) return p + (q - p) * 6 * t;
        if (t < 1f / 2) return q;
        if (t < 2f / 3) return p + (q - p) * (2f / 3 - t) * 6;
        return p;
    }
}
