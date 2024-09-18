package it.bancaditalia.oss.sdmx.api;

import it.bancaditalia.oss.sdmx.util.LocalizedText;

import java.util.Map;

public class Concept extends SDMXReference {
    private LocalizedText name = null;
    private LocalizedText description = null;
    private String coreRepresentationFullId = null;

    public Concept(final String id, final String agency, final String version) {
        super(id, agency, version);
        name = null;
        description = null;
        coreRepresentationFullId = null;
    }

    public Concept(final SDMXReference coordinates,
                   final LocalizedText name,
                   final LocalizedText description,
                   final String coreRepresentationFullId) {
        super(coordinates);
        this.name = name;
        this.description = description;
        this.coreRepresentationFullId = coreRepresentationFullId;
    }

    public String getName() {
        return name != null ? name.getText() : "";
    }

    public String getDescription() {
        return description != null ? description.getText() : null;
    }

    public String getCoreRepresentationFullId() {
        return coreRepresentationFullId != null ? coreRepresentationFullId : "";
    }
}
