package de.intranda.goobi.plugins;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopConfParser;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.MimeConstants;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IWorkflowPlugin;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.StorageProvider;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;

/**
 * This plugin is used to generate barcodes. It is implemented as Goobi workflow plugin so that it is accessible to all users with access to the menu
 * 'workflow'. It does not create any kind of processes and it does not change any information inside of the Goobi workflow database.
 * 
 * @author Steffen Hankiewicz
 */
@PluginImplementation
@Log4j
@Data
public class BarcodeGeneratorPlugin implements IWorkflowPlugin, IPlugin {
    private static final long serialVersionUID = 2253037160388668479L;

    private static final String PLUGIN_NAME = "intranda_workflow_barcode_generator";

    private String xsltFile;
    private String prefix;
    private int startNumber;
    private int amount;
    private String format;
    private String separator;

    /**
     * Constructor
     */
    public BarcodeGeneratorPlugin() {
        log.info("Barcode generation plugin started");
        initialize();
    }

    /**
     * general initialisation
     */
    private void initialize() {
        xsltFile = ConfigurationHelper.getInstance().getXsltFolder() + ConfigPlugins.getPluginConfig(PLUGIN_NAME)
                .getString("xslt-file",
                        "barcode.xslt");
        prefix = ConfigPlugins.getPluginConfig(PLUGIN_NAME).getString("prefix", "");
        separator = ConfigPlugins.getPluginConfig(PLUGIN_NAME).getString("separator", "_");
        startNumber = ConfigPlugins.getPluginConfig(PLUGIN_NAME).getInt("start", 1);
        amount = ConfigPlugins.getPluginConfig(PLUGIN_NAME).getInt("amount", 10);
        format = ConfigPlugins.getPluginConfig(PLUGIN_NAME).getString("format", "00000000");
    }

    @Override
    public PluginType getType() {
        return PluginType.Workflow;
    }

    @Override
    public String getTitle() {
        return PLUGIN_NAME;
    }

    @Override
    public String getGui() {
        return "/uii/plugin_workflow_barcodeGenerator.xhtml";
    }

    public void cancel() {
        log.info("Barcode generation cancelled");
        initialize();
    }

    /**
     * offer a list of all available xsl files to select from
     */
    public List<SelectItem> getXsltFiles() {
        List<SelectItem> pathes = new ArrayList<>();
        for (String s : Arrays.asList(ConfigPlugins.getPluginConfig(PLUGIN_NAME).getStringArray("xslt-file"))) {
            pathes.add(new SelectItem(ConfigurationHelper.getInstance().getXsltFolder() + s));
        }
        return pathes;
    }

    /**
     * Generate barcodes as PDF file based on given parameters from the users form
     */
    public void generateBarcodes() {
        log.debug("Barcode generation started");
        log.debug("prefix: " + prefix);
        log.debug("separator: " + separator);
        log.debug("startNumber: " + startNumber);
        log.debug("amount: " + amount);
        log.debug("xsltFile: " + xsltFile);

        try {
            // prepare the xml source
            Element mainElement = new Element("process");
            Document doc = new Document(mainElement);
            Namespace xmlns = Namespace.getNamespace("http://www.goobi.io/logfile");
            mainElement.setNamespace(xmlns);

            // namespace declaration
            Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            mainElement.addNamespaceDeclaration(xsi);
            Attribute attSchema = new Attribute("schemaLocation", "http://www.goobi.io/logfile" + " XML-logfile.xsd",
                    xsi);
            mainElement.setAttribute(attSchema);

            // process information
            for (int i = 0; i < amount; i++) {
                Element process = new Element("item", xmlns);
                DecimalFormat df = new DecimalFormat(format);
                String current = df.format(startNumber + i);
                if (prefix.isEmpty()) {
                    process.setText(current);
                } else {
                    process.setText(prefix + separator + current);
                }
                mainElement.addContent(process);
            }

            // write xml to harddisk for testing
            //	        XMLOutputter xout = new XMLOutputter();
            //	        Format f = Format.getPrettyFormat();
            //	        f.setEncoding("UTF-8");
            //	        xout.setFormat(f);
            //	        xout.output(doc, new FileOutputStream(ConfigurationHelper.getInstance().getTemporaryFolder() + "barcode.xml"));

            // write xml to bytearray
            XMLOutputter outp = new XMLOutputter();
            outp.setFormat(Format.getPrettyFormat());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            outp.output(doc, out);
            out.close();

            // find xslt file
            Path xsltfile = Paths.get(xsltFile);
            if (!StorageProvider.getInstance().isFileExists(xsltfile)) {
                Helper.setFehlerMeldung("plugin_barcode_generation_missingXslFile");
                return;
            }

            // read xslt configuration for fonts etc.
            StreamSource source = new StreamSource(new ByteArrayInputStream(out.toByteArray()));
            File xconf = new File(ConfigurationHelper.getInstance().getXsltFolder() + "config.xml");
            FopConfParser parser = null;
            try {
                parser = new FopConfParser(xconf);
            } catch (Exception e) {
                log.error(e);
                return;
            }

            // generate pdf file based on xslt file
            FopFactoryBuilder builder = parser.getFopFactoryBuilder();
            FopFactory fopFactory = builder.build();
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            try {
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer transformer = factory.newTransformer(new StreamSource(xsltfile.toString()));
                Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, outStream);
                Result res = new SAXResult(fop.getDefaultHandler());
                transformer.transform(source, res);
            } catch (FOPException e) {
                throw new IOException("FOPException occured", e);
            } catch (TransformerException e) {
                throw new IOException("TransformerException occured", e);
            }
            byte[] pdfBytes = outStream.toByteArray();

            // send the generated pdf to servlet output stream
            FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
            if (!facesContext.getResponseComplete()) {
                HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
                String fileName = prefix + "_barcodes.pdf";
                if (prefix.isEmpty()) {
                    fileName = "goobi_barcodes.pdf";
                }
                ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
                String contentType = servletContext.getMimeType(fileName);
                response.setContentType(contentType);
                response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
                ServletOutputStream os = response.getOutputStream();
                os.write(pdfBytes);
                os.flush();
                FacesContext.getCurrentInstance().responseComplete();
            }

        } catch (Exception e) {
            log.error("Error occured while generating the barcodes", e);
        }
    }
}
