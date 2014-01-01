/*
 * #%L
 * SCIFIO OME-XML Formats.
 * %%
 * Copyright (C) 2013 - 2014 Open Microscopy Environment:
 *   - Massachusetts Institute of Technology
 *   - National Institutes of Health
 *   - University of Dundee
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package io.scif.ome.xml.meta;

import io.scif.AbstractSCIFIOPlugin;
import io.scif.common.Constants;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import ome.xml.DOMUtil;
import ome.xml.model.OME;
import ome.xml.model.OMEModelObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A utility class for constructing and manipulating OME-XML DOMs. It is the
 * superclass for all versions of OME-XML. It requires the ome.xml package to
 * compile (part of ome-xml.jar).
 * <dl>
 * <dt><b>Source code:</b></dt>
 * <dd><a href=
 * "http://trac.openmicroscopy.org.uk/ome/browser/bioformats.git/components/bio-formats/src/loci/formats/ome/AbstractOMEXMLMetadata.java"
 * >Trac</a>, <a href=
 * "http://git.openmicroscopy.org/?p=bioformats.git;a=blob;f=components/bio-formats/src/loci/formats/ome/AbstractOMEXMLMetadata.java;hb=HEAD"
 * >Gitweb</a></dd>
 * </dl>
 * 
 * @author Curtis Rueden ctrueden at wisc.edu
 * @author Melissa Linkert melissa at glencoesoftware.com
 */
public abstract class AbstractOMEXMLMetadata extends AbstractSCIFIOPlugin
	implements OMEXMLMetadata
{

	// -- Constants --

	/** XSI namespace. */
	public static final String XSI_NS =
		"http://www.w3.org/2001/XMLSchema-instance";

	/** OME-XML schema location. */
	public static final String SCHEMA =
	// "http://www.openmicroscopy.org/Schemas/OME/2012-06/ome.xsd";
		"https://raw.github.com/openmicroscopy/openmicroscopy/schema-2012-06/components/specification/InProgress/ome.xsd";

	// -- Fields --

	/** The root element of OME-XML. */
	protected OMEModelObject root;

	/** DOM element that backs the first Image's CustomAttributes node. */
	private Element imageCA;

	private DocumentBuilder builder;

	// -- Constructors --

	/** Creates a new OME-XML metadata object. */
	public AbstractOMEXMLMetadata() {}

	// -- OMEXMLMetadata API methods --

	@Override
	public String dumpXML() {
		if (root == null) {
			root = (OMEModelObject) getRoot();
			if (root == null) return null;
		}
		try {
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			final Document doc = createNewDocument();
			final Element r = root.asXMLElement(doc);
			r.setAttribute("xmlns:xsi", XSI_NS);
			r.setAttribute("xsi:schemaLocation", OME.NAMESPACE + " " + SCHEMA);
			doc.appendChild(r);
			DOMUtil.writeXML(os, doc);
			return os.toString(Constants.ENCODING);
		}
		catch (final TransformerException exc) {
			log().warn("Failed to create OME-XML", exc);
		}
		catch (final UnsupportedEncodingException exc) {
			log().warn("Failed to create OME-XML", exc);
		}
		return null;
	}

	// -- MetadataRetrieve API methods --

	@Override
	public String getUUID() {
		final Element ome = getRootElement();
		return DOMUtil.getAttribute("UUID", ome);
	}

	// -- MetadataStore API methods --

	@Override
	public void setRoot(final Object root) {}

	@Override
	public Object getRoot() {
		return root;
	}

	@Override
	public void setUUID(final String uuid) {
		final Element ome = getRootElement();
		DOMUtil.setAttribute("UUID", uuid, ome);
	}

	// -- Type conversion methods --

	/**
	 * Converts Boolean value to Integer. Used to convert from 2003-FC Laser
	 * FrequencyDoubled Boolean value to Laser FrequencyMultiplication Integer
	 * value.
	 */
	protected Integer booleanToInteger(final Boolean value) {
		return value == null ? null : new Integer(value.booleanValue() ? 2 : 1);
	}

	/**
	 * Converts Integer value to Boolean. Used to convert from Laser
	 * FrequencyMultiplication Integer value to 2003-FC Laser FrequencyDoubled
	 * Boolean value.
	 */
	protected Boolean integerToBoolean(final Integer value) {
		return value == null ? null : new Boolean(value.intValue() == 2);
	}

	/**
	 * Converts Double value to Integer. Used to convert from 2008-02
	 * LogicalChannel PinholeSize Integer value to LogicalChannel PinholeSize
	 * Double value.
	 */
	protected Integer doubleToInteger(final Double value) {
		return value == null ? null : new Integer(value.intValue());
	}

	/**
	 * Converts Integer value to Double. Used to convert from LogicalChannel
	 * PinholeSize Double value to 2008-02 LogicalChannel PinholeSize Integer
	 * value.
	 */
	protected Double integerToDouble(final Integer value) {
		return value == null ? null : new Double(value.doubleValue());
	}

	// -- Helper methods --

	private Document createNewDocument() {
		if (builder == null) {
			try {
				builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			}
			catch (final ParserConfigurationException e) {}
		}
		return builder.newDocument();
	}

	private Element getRootElement() {
		return root.asXMLElement(createNewDocument());
	}

}
