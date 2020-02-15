package de.intranda.goobi.plugins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
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
import lombok.Data;
import lombok.extern.log4j.Log4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j
@Data
public class BarcodeGeneratorPlugin implements IWorkflowPlugin, IPlugin {
    private static final String PLUGIN_NAME = "intranda_workflow_barcode-generator";
    
    private String xsltPath;
    private String institutionCode;
    private int startNumber;
    private int amount;
    
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
    	xsltPath = ConfigPlugins.getPluginConfig(PLUGIN_NAME).getString("xslt-path", ConfigurationHelper.getInstance().getXsltFolder() + "barcode.xslt");
    	institutionCode = "";
    	startNumber = 1;
    	amount = 10;
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
     * Generate barcodes as PDF file based on given parameters from the users form
     */
    public void generateBarcodes() {
    	log.debug("Barcode generation started");
    	log.debug("institutionCode: " + institutionCode);
    	log.debug("startNumber: " + startNumber);
    	log.debug("amount: " + amount);
    	log.debug("xsltPath: " + xsltPath);
    	
    	try {
    		// prepare the xml source
	        Element mainElement = new Element("process");
	        Document doc = new Document(mainElement);
	        Namespace xmlns = Namespace.getNamespace("http://www.goobi.io/logfile");
	        mainElement.setNamespace(xmlns);
	
	        // namespace declaration
	        Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	        mainElement.addNamespaceDeclaration(xsi);
	        Attribute attSchema = new Attribute("schemaLocation", "http://www.goobi.io/logfile" + " XML-logfile.xsd", xsi);
	        mainElement.setAttribute(attSchema);
	        
	        // process information
	        for (int i = 0; i < amount;i++) {
	        	Element process = new Element("item", xmlns);
	        	process.setText(institutionCode + "_" + (startNumber + i));
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
	    	Path xsltfile = Paths.get(xsltPath);
	        if (!StorageProvider.getInstance().isFileExists(xsltfile)) {
	            Helper.setFehlerMeldung("docketMissing");
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
	            String fileName = institutionCode + "_barcodes.pdf";
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
