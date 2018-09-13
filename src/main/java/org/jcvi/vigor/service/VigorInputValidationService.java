package org.jcvi.vigor.service;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jcvi.vigor.utils.ConfigurationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class VigorInputValidationService {

	private static final Logger LOGGER = LogManager.getLogger(VigorInputValidationService.class);

	@Autowired
	private VigorInitializationService vigorInitializationServiceObj;

	public Namespace processInput(String... inputParams)  {
		ArgumentParser parser = getArgumentParser();
		return parser.parseArgsOrFail(inputParams);
	}

	private ArgumentParser getArgumentParser() {

		ArgumentParser parser = ArgumentParsers.newFor("vigor4")
											   .build()
											   .usage("${prog} -i inputfasta -o outputprefix [ -d refdb ]")
				.epilog(String.join("\n","Outputs:",
						"  outputprefix.rpt   - summary of program results",
						"  outputprefix.cds   - fasta file of predicted CDSs",
						"  outputprefix.pep   - fasta file of predicted proteins",
						"  outputprefix.tbl   - predicted features in GenBank tbl format",
						"  outputprefix.aln   - alignment of predicted protein to reference, and reference protein to genome"
						));

		MutuallyExclusiveGroup inputGroup = parser.addMutuallyExclusiveGroup().required(true);
		MutuallyExclusiveGroup outputGroup = parser.addMutuallyExclusiveGroup().required(true);
		MutuallyExclusiveGroup referenceGroup = parser.addMutuallyExclusiveGroup("reference database");
		MutuallyExclusiveGroup locusGroup = parser.addMutuallyExclusiveGroup("locus tag usage");

		ArgumentGroup unImplementedGroup = parser.addArgumentGroup("unimplemented");

		inputGroup.addArgument("-i","--input-fasta")
				  .action(Arguments.store())
				  .dest(CommandLineParameters.inputFile)
				  .metavar("<input fasta>")
				  .help("path to fasta file of genomic sequences to be annotated, (-I is a synonym for this option)");

		inputGroup.addArgument("-I")
				  .action(Arguments.store())
				  .dest(CommandLineParameters.inputFile)
				  .metavar("<input fasta>")
				  .help("synonym for -i/--input-fasta)");

		outputGroup.addArgument("-o","--output-prefix")
				   .action(Arguments.store())
				   .dest(CommandLineParameters.outputPrefix)
				   .metavar("<output prefix>")
				   .help("prefix for outputfile files, e.g. if the ouput prefix is /mydir/anno VIGOR will create output files /mydir/anno.tbl, /mydir/anno.stats, etc., (-O is a synonym for this option)");

		outputGroup.addArgument("-O")
				   .action(Arguments.store())
				   .dest(CommandLineParameters.outputPrefix)
				   .metavar("<output prefix>")
				   .help("synonym for -o/--output-prefix");

		unImplementedGroup.addArgument("-a", "--autoselect-reference")
					  .dest(CommandLineParameters.referenceDB)
					  .action(Arguments.storeConst())
					  .setConst("any")
					  .help("auto-select the reference database, equivalent to '-d any ', default behavior unless overridden by -d or -G, (-A is a synonym for this option). This feature is not yet implemented");
		unImplementedGroup.addArgument("-A")
					  .dest(CommandLineParameters.referenceDB)
					  .action(Arguments.storeConst())
					  .setConst("any")
					  .help("synonym for -a/--autoselect-reference");

		referenceGroup.addArgument("-d", "--reference-database")
					  .dest(CommandLineParameters.referenceDB)
					  .action(Arguments.store())
					  .metavar("<ref db>")
					  .help("specify the reference database to be used, (-D is a synonym for this option)");
		referenceGroup.addArgument("-D")
					  .dest(CommandLineParameters.referenceDB)
					  .action(Arguments.store())
					  .metavar("<ref db>")
					  .help("synonym for -d/--reference-database");
		unImplementedGroup.addArgument("-G", "--genbank-reference")
					  .metavar("<genback file>")
					  .dest(CommandLineParameters.genbankDB)
					  .action(Arguments.store())
					  .help("use a genbank file as the reference database, caution: VIGOR genbank parsing is fairly rudimentary and many genbank files are unparseable.  Partial genes will be ignored. Note: genbank files do not record enough information to handle RNA editing. This feature is not yet implemented.");

		// TODO what are acceptable values for this
		unImplementedGroup.addArgument("-e", "--evalue")
			  .action(Arguments.store())
			  .type(Integer.class)
			  .dest(CommandLineParameters.eValue)
			  .help("override the default evalue used to identify potential genes, the default is usually 1E-5, but varies by reference database");

		// TODO add validation
		parser.addArgument("-c", "--min-coverage")
			  .dest(CommandLineParameters.minCoverage)
			  .action(Arguments.store())
			  .help("minimum coverage of reference product (0-100) required to report a gene, by default coverage is ignored");

		unImplementedGroup.addArgument("-C", "--complete")
				   .help("complete (linear) genome (do not treat edges as gaps). This feature is currently unimplemented")
				   .dest(CommandLineParameters.completeGene)
				   .action(Arguments.storeTrue());
		unImplementedGroup.addArgument("-0", "--circular")
				   .dest(CommandLineParameters.circularGene)
				   .help("complete circular genome (allows gene to span origin). This feature is currently unimplemented")
				   .action(Arguments.storeTrue());
		unImplementedGroup.addArgument("-f", "--frameshift-sensitivity")
			  .action(Arguments.store())
			  .dest(CommandLineParameters.frameshiftSensitivity)
			  .choices("0","1","2")
			  .setDefault("1")
			  .help("frameshift sensitivity, 0=ignore frameshifts, 1=normal (default), 2=sensitive. ");

		unImplementedGroup.addArgument("-K", "--skip-candidate-selection")
			  .choices("0", "1")
			  .setDefault("1")
			  .dest(CommandLineParameters.skipSelection)
			  .metavar("<value>")
			  .help("value=0 skip candidate selection (default=1)");

		// use storeConst rather than storeTrue to avoid automatically setting a default value
		locusGroup.addArgument("-l", "--no-locus-tags")
				  .dest(CommandLineParameters.locusTag)
				  .action(Arguments.storeConst())
				  .setConst("")
				  .help("do NOT use locus_tags in TBL file output (incompatible with -L)");
		locusGroup.addArgument("-L", "--locus-tags")
				  .dest(CommandLineParameters.locusTag)
				  .action(Arguments.store())
				  .nargs("?")
				  // default is used when the option is not present
				  .setDefault("vigor_")
				  // const is used when the option is present, but no argument is passed
				  .setConst("vigor_")
				  .metavar("<locus_tag_prefix>")
				  .help("USE locus_tags in TBL file output (incompatible with -l). If no prefix is provided, the prefix \"vigor_\" will be used.");

		parser.addArgument("-P","--parameter")
			  .action(Arguments.append())
			  .dest(CommandLineParameters.parameters)
			  .metavar("<parameter=value~~...~~parameter=value>")
			  .help("~~ separated list of VIGOR parameters to override default values. Use --list-config-parameters to see settable parameters.");
		unImplementedGroup.addArgument("-j", "--jcvi-rules-off")
			  .action(Arguments.storeFalse())
			  .dest(CommandLineParameters.jcviRules)
			  .setDefault(true)
			  .help("turn off JCVI rules, JCVI rules treat gaps and ambiguity codes conservatively, use this option to relax these constraints and produce a more speculative annotation");
		unImplementedGroup.addArgument("-m","--ignore-reference-requirements")
			  .action(Arguments.storeTrue())
			  .dest(CommandLineParameters.ignoreReferenceRequirements)
			  .help("ignore reference match requirements (coverage/identity/similarity), sometimes useful when running VIGOR to evaluate raw contigs and rough draft sequences");
		parser.addArgument("-s","--min-gene-size")
			  .action(Arguments.store())
			  .dest(CommandLineParameters.minGeneSize)
			  .type(Integer.class)
			  .metavar("<gene size>")
			  .help("minimum size (aa) of product required to report a gene, by default size is ignored");
		parser.addArgument("-v","--verbose")
			  .action(Arguments.count())
			  .dest(CommandLineParameters.verbose)
			  .help("verbose logging (default=terse)");

		unImplementedGroup.addArgument("-x", "--ignore-refID")
			  .action(Arguments.append())
			  .dest(CommandLineParameters.ignoreRefID)
			  .metavar("<ref_id,...,ref_id>")
			  .help("comma separated list of reference sequence IDs to ignore (useful when debugging a reference database). Not currently implemented");

		parser.addArgument("--list-config-parameters")
			  .action(new ListConfigurations())
			  .help("list available configuration parameters and exit");

		parser.addArgument("--version")
			  .action(new PrintVersion())
			  .help("print version information");

		parser.addArgument("--config-file")
			  .action(Arguments.store())
			  .dest(CommandLineParameters.configFile)
			  .help("config file to use");

		parser.addArgument("--reference-database-path")
			  .action(Arguments.store())
			  .dest(CommandLineParameters.referenceDB_Path)
			  .help("reference database path");

		parser.addArgument("--virus-config")
			  .action(Arguments.store())
			  .dest(CommandLineParameters.virusSpecificConfig)
			  .help("Path to virus specific configuration");

		parser.addArgument("--virus-config-path")
			  .action(Arguments.store())
			  .dest(CommandLineParameters.virusSpecificConfigPath)
			  .help("Path to directory containing virus specific config files.");

        parser.addArgument("--overwrite-output")
              .action(Arguments.storeTrue())
              .dest(CommandLineParameters.overwriteOutputFiles)
              .help("overwrite existing output files if they exist");

        parser.addArgument("--temporary-directory")
			  .action(Arguments.store())
			  .dest(CommandLineParameters.temporaryDirectory)
			  .help("Root directory to use for temporary directories");

		return parser;
	}

	private static abstract class CustomNoArgumentAction implements ArgumentAction {

		@Override
		public void onAttach(Argument argument) {
		}

		@Override
		public boolean consumeArgument() {
			return false;
		}

	}

	private static class PrintVersion extends CustomNoArgumentAction {

		@Override
		public void run(ArgumentParser argumentParser, Argument argument, Map<String, Object> map, String s, Object o) throws ArgumentParserException {
			try {
				Properties gitProperties = new Properties();
				gitProperties.load(this.getClass().getResourceAsStream("/git.properties"));
				System.out.println(String.format("%s-%s (branch %s) (built on host %s at %s)",
												 gitProperties.getProperty("git.build.version"),
												 gitProperties.getProperty("git.commit.id.abbrev"),
												 gitProperties.getProperty("git.branch"),
												 gitProperties.getProperty("git.build.host"),
												 gitProperties.getProperty("git.build.time")
								   )
				);
				System.exit(0);
			} catch (IOException e ) {
				throw new ArgumentParserException(argumentParser);
			}
		}
	}
	private static class ListConfigurations extends CustomNoArgumentAction{

		@Override
		public void run(ArgumentParser argumentParser, Argument argument, Map<String, Object> map, String s, Object o) throws ArgumentParserException {
			System.out.println("Configuration Parameters\n");
			for (ConfigurationParameters param: Arrays.stream(ConfigurationParameters.values())
													  .sorted(Comparator.comparing(p-> p.configKey, String.CASE_INSENSITIVE_ORDER))
													  .filter(p -> p.hasFlag(ConfigurationParameters.Flags.VERSION_4))
													  .collect(Collectors.toList())) {
				System.out.println(param.configKey);
				System.out.println();
				System.out.println(String.format("\t%-30s VIGOR_%s","Environment variable:",param.configKey.toUpperCase()));
				System.out.println(String.format("\t%-30s vigor.%s","System.property:", param.configKey));
				if (! (param.description == null || param.description.isEmpty()) ) {
					System.out.println(String.format("\t%-30s %s", "Description:", param.description));
				}
				System.out.println();
			}
			System.exit(0);
		}
	}
}
