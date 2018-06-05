package org.jcvi.vigor.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.jcvi.jillion.core.Direction;
import org.jcvi.jillion.core.Range;
import org.jcvi.jillion.core.residue.Frame;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jcvi.vigor.Application;
import org.jcvi.vigor.component.AlignmentFragment;
import org.jcvi.vigor.component.Exon;
import org.jcvi.vigor.component.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by snettem on 5/19/2017.
 */


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = Application.class)
public class ModelGenerationServiceTest {

	@Autowired
	private ModelGenerationService modelGenerationService;

	@Test
	public void testClone() throws CloneNotSupportedException {
		Model model = new Model();
		Exon exon = new Exon();
		exon.setRange(Range.of(2,3));
		List<Exon> exons = new ArrayList<Exon>();
		exons.add(exon);
		model.setExons(exons);
		Model clonedModel = model.clone();
		assertEquals("Cloned model should have the same exons",
				model.getExons().get(0).getRange(),
				clonedModel.getExons().get(0).getRange());
		model.getExons().get(0).setRange(Range.of(3,4));
		assertEquals("Changing an exon on the original model should not change exons in cloned models",
				Range.of(2,3),clonedModel.getExons().get(0).getRange());

	}

	@Test
	public void splitModelAtSequenceGapsTest() throws CloneNotSupportedException {
		Model model = new Model();
		List<Exon> exons = new ArrayList<Exon>();
		exons.add(new Exon(Range.of(20, 500),Frame.ONE));
		exons.add(new Exon(Range.of(600,1300),Frame.TWO));
		exons.add(new Exon(Range.of(1600,2800),Frame.ONE));
		exons.add(new Exon(Range.of(3011,4500),Frame.ONE));
		model.setExons(exons);
		model.setStatus(new ArrayList<String>());
		List<Range> sequenceGaps = new ArrayList<Range>();
		sequenceGaps.add(Range.of(1400,1554));
		sequenceGaps.add(Range.of(2938,3010));
		sequenceGaps.add(Range.of(4550,4904));
		sequenceGaps.add(Range.of(8508,8698));
		sequenceGaps.add(Range.of(9906,9965));
		sequenceGaps.add(Range.of(11619,11759));
		List<Model> models = modelGenerationService.splitModelAtSequenceGaps(model, sequenceGaps);
		assertEquals(3,models.size());

	}

	@Test
	public void generateCompatibleFragsChainsTest(){
		List<AlignmentFragment> alignmentFrags = new ArrayList<AlignmentFragment>();
		alignmentFrags.add(new AlignmentFragment(Range.of(0,100),Range.of(10,300),1000,Direction.FORWARD,Frame.ONE));
		alignmentFrags.add(new AlignmentFragment(Range.of(60,120),Range.of(180,360),1000,Direction.FORWARD,Frame.ONE));
		alignmentFrags.add(new AlignmentFragment(Range.of(65,110),Range.of(195,330),1000,Direction.FORWARD,Frame.ONE));
		alignmentFrags.add(new AlignmentFragment(Range.of(121,150),Range.of(370,460),1000,Direction.FORWARD,Frame.ONE));
		alignmentFrags.add(new AlignmentFragment(Range.of(130,170),Range.of(330,500),1000,Direction.FORWARD,Frame.ONE));
		alignmentFrags.add(new AlignmentFragment(Range.of(171,180),Range.of(650,670),1000,Direction.FORWARD,Frame.ONE));
		List<List<AlignmentFragment>> outList = modelGenerationService.generateCompatibleFragsChains(alignmentFrags, "exonerate");
		assertEquals(outList.size(),6);
	}
}
