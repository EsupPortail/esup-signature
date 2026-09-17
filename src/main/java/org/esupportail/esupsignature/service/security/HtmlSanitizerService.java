package org.esupportail.esupsignature.service.security;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class HtmlSanitizerService {

    private static final Safelist SUMMERNOTE_SAFE_LIST = Safelist.relaxed()
            .addTags("span", "u", "s", "strike")
            .addAttributes(":all", "class", "style")
            .addAttributes("a", "target")
            .addAttributes("img", "alt", "height", "width")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("blockquote", "cite", "http", "https")
            .addProtocols("img", "src", "http", "https", "data")
            .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer");

    private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings()
            .prettyPrint(false);

    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, "", SUMMERNOTE_SAFE_LIST, OUTPUT_SETTINGS);
    }
}
