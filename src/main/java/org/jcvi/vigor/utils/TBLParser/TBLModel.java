package org.jcvi.vigor.utils.TBLParser;

import java.util.List;

import org.jcvi.vigor.component.Exon;

import lombok.Data;

@Data
public class TBLModel {
	
	private String virusGenomeID;
	private List<Exon> exons;
	private String viralProteinID;
	private String product;
	private String note;
	private String gene;

}
