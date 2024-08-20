---
title: PDF-Export in Verzeichnisstruktur der NLI
identifier: intranda_export_nli_pdf_to_folder_structure
description: Export Plugin für den Export von PDF-Dateien mit besonderer Ordner- und Dateibenennung für die Nationalbibliothek Israel.
published: true
---

## Einführung
Diese Dokumentation erläutert das Plugin für den Export von PDF-Dateien mit besonderer Ordner- und Dateibenennung für die Nationalbibliothek Israel. Das Plugin erzeugt hierfür in einem definierten Verzeichnis die ggf. benötigten Unterordner und speichert eine vorhandene PDF-Datei aus dem Master-Ordner mit dem gewünschten Namen innerhalb der erzeugten Ordnerstruktur.

## Installation
Um das Plugin nutzen zu können, müssen folgende Dateien installiert werden:

```bash
/opt/digiverso/goobi/plugins/export/plugin-export-nli-pdf-to-folder-structure-base.jar
/opt/digiverso/goobi/config/plugin_intranda_export_nli_pdf_to_folder_structure.xml
```

Nach der Installation des Plugins kann dieses innerhalb des Workflows für die jeweiligen Arbeitsschritte ausgewählt und somit automatisch ausgeführt werden. Ein Workflow könnte dabei beispielhaft wie folgt aussehen:

![Beispielhafter Aufbau eines Workflows](screen1_de.png)

Für die Verwendung des Plugins muss dieses in einem Arbeitsschritt ausgewählt sein:

![Konfiguration des Arbeitsschritts für die Nutzung des Plugins](screen2_de.png)


## Überblick und Funktionsweise
Das Plugin wird im Verlauf des Workflows automatisch ausgeführt und liest die Parameter aus der Konfigurationsdatei. Auf dieser Basis ermittelt das Plugin anschließend Metadaten aus dem jeweiligen Vorgang. Die somit ermittelten Informationen werden anschließend dafür genutzt, einen Verzeichnispfad für den Export zu generieren und das Verzeichnis zu erzeugen, wenn es nicht bereits existieren sollte. Anschließend generiert das Plugin einen Dateinamen, der auf einen Zähler endet. Der somit erzeugte Dateiname wird daraufhin überprüft, ob er schon in Verwendung ist, so dass ggf. der Zähler angepasst wird, um einen noch nicht verwendeten Dateinamen zu erhalten. Anschließend wird aus dem Master-Verzeichnis des Goobi-Vorgangs das erste PDF-Dokument ermittelt und unter dem vorher generierten Dateinamen innerhalb des Verzeichnispfades gespeichert.


## Konfiguration
Die Konfiguration des Plugins erfolgt in der Datei `plugin_intranda_export_nli_pdf_to_folder_structure.xml` wie hier aufgezeigt:

{{CONFIG_CONTENT}}

Die darin verwendeten Parameter werden hier beschrieben: 

Parameter                | Erläuterung
-------------------------|------------------------------------
`exportFolder`           | Hauptverzeichnis für den Export (z.B. `/opt/digiverso/export`)
`metdataPublicationDate` | Metadatum für das Veröffentlichungsdatum; es kann hier die Syntax für den VariablenReplacer verwendet werden (z.B. `$(meta.DateOfOrigin)`)
`metdataPublicationCode` | Metadatum für den Publikationscode; es kann hier die Syntax für den VariablenReplacer verwendet werden (z.B. `$(meta.Type)`)
`dateReadPattern`        | Pattern für das Einlesen des Veröffentlichungsdatum (z.B. `yyyy-MM-dd`)
`dateWritePattern`       | Pattern für das Schreiben des aktuellen und des Veröffentlichungsdatums (z.B. `ddMMyyyy`)

