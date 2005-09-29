package org.genepattern.io.expr.mage;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import org.biomage.BioAssay.BioAssay;
import org.biomage.BioAssay.BioAssay_package;
import org.biomage.BioAssay.DerivedBioAssay;
import org.biomage.BioAssayData.BioAssayData;
import org.biomage.BioAssayData.BioAssayData_package;
import org.biomage.BioAssayData.BioAssayDimension;
import org.biomage.BioAssayData.BioDataCube;
import org.biomage.BioAssayData.DataExternal;
import org.biomage.BioAssayData.DataInternal;
import org.biomage.BioAssayData.DerivedBioAssayData;
import org.biomage.BioAssayData.QuantitationTypeDimension;
import org.biomage.BioAssayData.ReporterDimension;
import org.biomage.Common.MAGEJava;
import org.biomage.Description.Description;
import org.biomage.DesignElement.DesignElement_package;
import org.biomage.DesignElement.Reporter;
import org.biomage.Interface.HasDescriptions.Descriptions_list;
import org.biomage.QuantitationType.DerivedSignal;
import org.biomage.QuantitationType.PresentAbsent;
import org.biomage.QuantitationType.QuantitationType_package;
import org.biomage.tools.xmlutils.PCData;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.expr.IResExpressionData;
import org.genepattern.io.expr.IExpressionDataWriter;

/**
 * Writes MAGE-ML documents.
 * 
 * @author Joshua Gould
 */
public class MAGEMLWriter implements IExpressionDataWriter {
	final static String FORMAT_NAME = "MAGE-ML";

	String externalDataFilenameURI = "MAGE-ML-data.txt";

	public String checkFileExtension(String filename) {
		if (!filename.toLowerCase().endsWith(".xml")) {
			filename += ".xml";
		}
		return filename;
	}

	public void write(IExpressionData expressionData, OutputStream os)
			throws IOException {
		boolean storeDataExternally = true;
		final MAGEJava mageJava = new MAGEJava() {
			public void writeAttributes(Writer out) throws IOException {
				out.write(" identifier=\"MAGE-ML:1\"");
			}
		};

		QuantitationType_package quantitationType_package = new QuantitationType_package();
		mageJava.setQuantitationType_package(quantitationType_package);
		QuantitationTypeDimension quantitationTypeDimension = new QuantitationTypeDimension();
		quantitationTypeDimension.setIdentifier("QT:1");

		DerivedSignal avgDiffQuantitationType = new DerivedSignal();
		avgDiffQuantitationType
				.setIdentifier("Affymetrix:QuantitationType:CHPAvgDiff");
		avgDiffQuantitationType.setName("Avg Diff");
		avgDiffQuantitationType.setIsBackground(Boolean.FALSE);

		quantitationTypeDimension
				.addToQuantitationTypes(avgDiffQuantitationType);
		quantitationType_package
				.addToQuantitationType_list(avgDiffQuantitationType);

		if (expressionData instanceof IResExpressionData) {
			PresentAbsent callQuantitationType = new PresentAbsent();
			callQuantitationType
					.setIdentifier("Affymetrix:QuantitationType:CHPAbsCall");
			callQuantitationType.setName("Abs Call");
			callQuantitationType.setIsBackground(Boolean.FALSE);

			Descriptions_list descriptions_list = new Descriptions_list();
			Description d = new Description();
			d
					.setText("Indication of the confidence in the measured expression value");
			descriptions_list.add(d);
			callQuantitationType.setDescriptions(descriptions_list);

			quantitationTypeDimension
					.addToQuantitationTypes(callQuantitationType);
			quantitationType_package
					.addToQuantitationType_list(callQuantitationType);
		}

		DesignElement_package designElement_package = new DesignElement_package();
		mageJava.setDesignElement_package(designElement_package);
		ReporterDimension designElementDimension = new ReporterDimension();
		designElementDimension.setIdentifier("DED:1");
		for (int i = 0; i < expressionData.getRowCount(); i++) {
			Reporter reporter = new Reporter();
			String desc = expressionData.getRowDescription(i);
			/*
			 * if(desc != null) { Descriptions_list descriptions_list = new
			 * Descriptions_list(); Description d = new Description();
			 * d.setText(java.net.URLEncoder.encode(desc, "UTF-8"));
			 * descriptions_list.add(d);
			 * reporter.setDescriptions(descriptions_list); } catch(Exception x)
			 * {x.printStackTrace();} }
			 */
			reporter.setIdentifier(expressionData.getRowName(i));
			reporter.setName(expressionData.getRowName(i));
			designElementDimension.addToReporters(reporter);
			designElement_package.addToReporter_list(reporter);
		}

		BioAssayDimension bioAssayDimension = new BioAssayDimension();
		bioAssayDimension.setIdentifier("BAD:1");
		BioAssay_package bioAssay_package = new BioAssay_package();
		mageJava.setBioAssay_package(bioAssay_package);
		for (int j = 0; j < expressionData.getColumnCount(); j++) {
			BioAssay mba = new DerivedBioAssay();
			mba.setIdentifier(expressionData.getColumnName(j));
			/*
			 * String desc = expressionData.getColumnDescription(j); if(desc !=
			 * null) { Descriptions_list descriptions_list = new
			 * Descriptions_list(); Description d = new Description();
			 * d.setText(desc); descriptions_list.add(d);
			 * mba.setDescriptions(descriptions_list); }
			 */
			bioAssayDimension.addToBioAssays(mba);
			bioAssay_package.addToBioAssay_list(mba);
		}

		BioDataCube bioDataCube = new BioDataCube();

		IResExpressionData resExpressionData = null;
		if (expressionData instanceof IResExpressionData) {
			resExpressionData = (IResExpressionData) expressionData;
		}

		if (storeDataExternally) {
			DataExternal dataExternal = new DataExternal();
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(new FileWriter(externalDataFilenameURI));
				for (int i = 0; i < expressionData.getRowCount(); i++) {
					for (int j = 0; j < expressionData.getColumnCount(); j++) {
						if (j > 0) {
							pw.print("\t");
						}
						pw.print(expressionData.getValue(i, j));

						if (resExpressionData != null) {

							String call = resExpressionData.getCallAsString(i,
									j);

							pw.print("\t");
							pw.print(call);
						}
					}
					pw.println();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				if (pw != null) {
					pw.close();
				}
			}
			dataExternal.setFilenameURI(externalDataFilenameURI);
			dataExternal.setDataFormat("tab delimited");
			bioDataCube.setDataExternal(dataExternal);
		} else {
			DataInternal dataInternal = new DataInternal();
			StringBuffer buf = new StringBuffer();

			for (int j = 0; j < expressionData.getColumnCount(); j++) {
				for (int i = 0; i < expressionData.getRowCount(); i++) {
					buf.append(String.valueOf(expressionData.getValue(
							i, j)));
					buf.append(" ");
					if (resExpressionData != null) {
						String call = resExpressionData.getCallAsString(i, j);

						buf.append(call);
						buf.append(" ");
					}
				}
			}

			final String content = buf.toString();
			buf = null;
			PCData pcData = new PCData() {
				public void appendChars(char[] values, int from, int length) {
				}

				public void writeMAGEML(Writer out) throws IOException {
					out.write(content);
				}

				public String getContent() {
					return content;
				}
			};
			dataInternal.setPcData(pcData);
			bioDataCube.setDataInternal(dataInternal);
		}

		bioDataCube.setValueByNameOrder("BDQ");

		BioAssayData bioAssayData = new DerivedBioAssayData();
		bioAssayData.setIdentifier("BAD:1");
		bioAssayData.setQuantitationTypeDimension(quantitationTypeDimension);
		bioAssayData.setDesignElementDimension(designElementDimension);
		bioAssayData.setBioAssayDimension(bioAssayDimension);
		bioAssayData.setBioDataValues(bioDataCube);

		BioAssayData_package bioAssayData_package = new BioAssayData_package();
		mageJava.setBioAssayData_package(bioAssayData_package);
		bioAssayData_package.addToBioAssayData_list(bioAssayData);
		bioAssayData_package.addToBioAssayDimension_list(bioAssayDimension);
		bioAssayData_package
				.addToDesignElementDimension_list(designElementDimension);
		bioAssayData_package
				.addToQuantitationTypeDimension_list(quantitationTypeDimension);

		PrintWriter pw = new PrintWriter(os);
		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		pw.println("<!DOCTYPE MAGE-ML>");
		mageJava.writeMAGEML(pw);
		pw.flush();
		pw.close();
	}

	public void setExternalDataFilenameURI(String externalDataFilenameURI) {
		this.externalDataFilenameURI = externalDataFilenameURI;
	}

	public String getFormatName() {
		return FORMAT_NAME;
	}

}

