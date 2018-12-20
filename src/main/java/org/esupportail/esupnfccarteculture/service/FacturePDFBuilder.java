/**
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.esupnfccarteculture.service;

import java.math.BigDecimal;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.esupnfccarteculture.domain.FactureModel;
import org.esupportail.esupnfccarteculture.domain.TagLog;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;


public class FacturePDFBuilder extends AbstractITextPdfView {
	 
    @Override
    protected void buildPdfDocument(Map<String, Object> model, Document doc,
            PdfWriter writer, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        DateFormat dfHeure = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    	Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA);
        fontTitle.setSize(30);
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA);
        fontHeader.setStyle("bold");
        fontHeader.setColor(BaseColor.WHITE);
        // get data model which is passed by the Spring container
        
        FactureModel factureModel = (FactureModel) model.get("factureModel");
        
        List<TagLog> tagLogs = factureModel.getTagLogs();
        
        String path = request.getScheme() + "://"+request.getLocalAddr() + ":" + request.getServerPort() +"/resources/images/logo-facture.jpg";
        Image logo = Image.getInstance(new URL(path));
        doc.add(logo);
        doc.add(new Phrase("Carte culture", fontTitle));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Facture du "
        		+ dateFormat.format(factureModel.getDateDebut())
        		+ " au "
        		+ dateFormat.format(factureModel.getDateFin())
        		+ " pour la salle "
        		+ factureModel.getSalle()));
        doc.add(new Paragraph(" "));
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100.0f);
        table.setWidths(new float[] {3.0f, 3.0f, 2.0f});
        table.setSpacingBefore(10);
         
        // define font for table header row
         
        // define table header cell
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new BaseColor(238, 95, 35));
        cell.setPadding(5);
         
        // write table header
        //cell.setPhrase(new Phrase("Nom salle", fontHeader));
        //table.addCell(cell);
        cell.setPhrase(new Phrase("Nom etudiant", fontHeader));
        table.addCell(cell);
        cell.setPhrase(new Phrase("Date", fontHeader));
        table.addCell(cell);
        cell.setPhrase(new Phrase("Tarif", fontHeader));
        table.addCell(cell);
        DecimalFormat decimalFormat = new java.text.DecimalFormat("####.00");        
        BigDecimal total = BigDecimal.valueOf(0);
       
        // write table row data
        for (TagLog taglog : tagLogs) {
            //table.addCell(taglog.getSalleNom());
            table.addCell(taglog.getEtudiantNomPrenom());

            
            table.addCell(dfHeure.format(taglog.getDate()));
            table.addCell(String.valueOf(taglog.getTarifEuro()));
            
            total = total.add(BigDecimal.valueOf(taglog.getTarif()));
        }
        //table.addCell("");
        table.addCell("");
        table.addCell("");
        table.addCell(decimalFormat.format(total.divide(BigDecimal.valueOf(100))));
        
        doc.add(table);
         
    }
 
}