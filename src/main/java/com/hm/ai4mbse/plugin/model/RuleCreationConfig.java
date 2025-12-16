package com.hm.ai4mbse.plugin.model;

public class RuleCreationConfig {
    private String useCase;
    private String regeltitel;
    private String ziel;
    private String elementtypen;
    private String pakete;
    private String stereotypen;
    private String prueflogik;
    private String ausgabeformat;
    private String strenge;
    private String normen;
    private String beispielKorrekt;
    private String beispielFehlerhaft;
    private String sprache;
    private String persona;
    private String tonalitaet;

    // Getter & Setter
    public String getRegeltitel() { return regeltitel; }
    public void setRegeltitel(String regeltitel) { this.regeltitel = regeltitel; }
    public String getZiel() { return ziel; }
    public void setZiel(String ziel) { this.ziel = ziel; }
    public String getElementtypen() { return elementtypen; }
    public void setElementtypen(String elementtypen) { this.elementtypen = elementtypen; }
    public String getPakete() { return pakete; }
    public void setPakete(String pakete) { this.pakete = pakete; }
    public String getStereotypen() { return stereotypen; }
    public void setStereotypen(String stereotypen) { this.stereotypen = stereotypen; }
    public String getPrueflogik() { return prueflogik; }
    public void setPrueflogik(String prueflogik) { this.prueflogik = prueflogik; }
    public String getAusgabeformat() { return ausgabeformat; }
    public void setAusgabeformat(String ausgabeformat) { this.ausgabeformat = ausgabeformat; }
    public String getStrenge() { return strenge; }
    public void setStrenge(String strenge) { this.strenge = strenge; }
    public String getNormen() { return normen; }
    public void setNormen(String normen) { this.normen = normen; }
    public String getBeispielKorrekt() { return beispielKorrekt; }
    public void setBeispielKorrekt(String beispielKorrekt) { this.beispielKorrekt = beispielKorrekt; }
    public String getBeispielFehlerhaft() { return beispielFehlerhaft; }
    public void setBeispielFehlerhaft(String beispielFehlerhaft) { this.beispielFehlerhaft = beispielFehlerhaft; }
    public String getSprache() { return sprache; }
    public void setSprache(String sprache) { this.sprache = sprache; }
    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }
    public String getTonalitaet() { return tonalitaet; }
    public void setTonalitaet(String tonalitaet) { this.tonalitaet = tonalitaet; }

    public void setUseCase(String useCase) { this.useCase = useCase; }
    public String getUseCase() { return useCase; }
}