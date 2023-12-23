package it.bancaditalia.oss.sdmx.api;

import it.bancaditalia.oss.sdmx.util.LocalizedText;

import java.io.Serializable;
import java.util.*;

public class ConceptScheme extends SDMXReference implements Iterable<String>, Map<String, Concept>, Serializable {

    private LocalizedText name;
    private LocalizedText description;
    private final Map<String, Concept> concepts;

    public ConceptScheme(final String id, final String agency, final String version) {
        super(id, agency, version);
        name = null;
        description = null;
        concepts = new HashMap<>();
    }

    public ConceptScheme(final SDMXReference coordinates,
                         final LocalizedText name,
                         final LocalizedText description,
                         final Map<String, Concept> concepts) {
        super(coordinates);
        this.name = name;
        this.description = description;
        this.concepts = concepts;
    }

    public String getName() {
        return name != null ? name.getText() : null;
    }

    public String getDescription() {
        return description != null ? description.getText() : null;
    }

    public Map<String, Concept> getConcepts() {
        return concepts;
    }

    @Override
    public Iterator<String> iterator() {
        return keySet().iterator();
    }

    @Override
    public void clear() {
        concepts.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return concepts.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return concepts.containsValue(value);
    }

    @Override
    public Set<Entry<String, Concept>> entrySet() {
        return concepts.entrySet();
    }

    @Override
    public Concept get(Object key) {
        return concepts.get(key);
    }

    @Override
    public boolean isEmpty() {
        return concepts.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return concepts.keySet();
    }

    @Override
    public void putAll(Map<? extends String, ? extends Concept> m) {
        concepts.putAll(m);
    }

    @Override
    public Concept remove(Object key) {
        return concepts.remove(key);
    }

    @Override
    public int size() {
        return concepts.size();
    }

    @Override
    public Collection<Concept> values() {
        return concepts.values();
    }

    @Override
    public Concept put(String key, Concept value) {
        return concepts.put(key, value);
    }
}
