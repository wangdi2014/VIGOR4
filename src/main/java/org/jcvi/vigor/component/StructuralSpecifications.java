package org.jcvi.vigor.component;

import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by snettem on 5/9/2017.
 */
@Component
@Scope("prototype")
@Data
public class StructuralSpecifications implements Cloneable {

    private List<String> shared_cds = Collections.EMPTY_LIST;
    private boolean is_required;
    private List<String> excludes_gene = Collections.EMPTY_LIST;
    private Map<String, Integer> tiny_exon3 = Collections.EMPTY_MAP;
    private Map<String, Integer> tiny_exon5 = Collections.EMPTY_MAP;
    private int minFunctionalLength;

    protected Object clone () throws CloneNotSupportedException {

        return super.clone();
    }
}
