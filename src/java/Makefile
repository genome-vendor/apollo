# Apollo Makefile
# Note that you must have APOLLO_ROOT defined in your environment for this to work.
# APOLLO_ROOT should be the directory where you have apollo set up (i.e. the parent directory
# of src/java).
# 09/04/2003: JDK1.4 is now required in order to make apollo.  You will get mysterious errors
# if you try to compile with an older JDK.

# path to Java compiler
# JAVAC = javac

# Aritistic style reindenter program location
# maybe astyle could be checked into apollo?
ASTYLE = /users/searle/progs/astyle_1.13.6.1/astyle

# Defined later, after we define CLASSPATH
#JFLAGS javac compiler options

# would be nice if we could just automatically scoop up all jars in jars directory 
# excluding apollo.jar. dont know how to do that in a makefile
#APOLLO_CLASSPATH=${APOLLO_ROOT}/src/java/classfiles:${APOLLO_ROOT}/src/java:${APOLLO_ROOT}/jars/org.bdgp.jar:${APOLLO_ROOT}/jars/org.jdesktop.jar:${APOLLO_ROOT}/jars/junit.jar:${APOLLO_ROOT}/jars/psgr2.jar:${APOLLO_ROOT}/data:${APOLLO_ROOT}/jars/sax.jar:${APOLLO_ROOT}/jars/jaxp.jar:${APOLLO_ROOT}/jars/crimson.jar:${APOLLO_ROOT}/jars/biojava.jar:${APOLLO_ROOT}/jars/ensj.jar:${APOLLO_ROOT}/jars/ensj-compatibility-19.0.jar:${APOLLO_ROOT}/jars/log4j-1.2.14.jar:${APOLLO_ROOT}/jars/patbinfree153.jar:${APOLLO_ROOT}/jars/jakarta-oro-2.0.6.jar:${APOLLO_ROOT}/jars/te-common.jar

#location of jar files
JARDIR=$(APOLLO_ROOT)/jars

#location of apollo java sources
SRCDIR=$(APOLLO_ROOT)/src/java

# pick up all jars (except apollo.jar) from jars directory, rather than having
# to explicitly add them...
## get jars listing from jars directory
jars = $(wildcard ${JARDIR}/*.jar)
# replace spaces with colons
jarsWithColons = $(subst jar ,jar:,${jars})
## take out apollo.jar
jarsNoApolloJar = $(subst ${JARDIR}/apollo.jar:,,${jarsWithColons})

# prepend classfiles & src/java
APOLLO_CLASSPATH = ${SRCDIR}/classfiles:${SRCDIR}:${jarsNoApolloJar}


# compilation flags
JFLAGS = -d classfiles -O -classpath ${APOLLO_CLASSPATH}

# Default build
all	: classdir apollo jalview external org-bdgp org-jdesktop
	perl ${APOLLO_ROOT}/src/perl/Apollo/update-timestamp.pl

clean-all	: clean all
clean_all	: clean all

# deletes compiled class files
clean	:	
	find ./apollo -name "*.class" -exec rm {} \;
	find ./edu -name "*.class" -exec rm {} \;
	find ./org -name "*.class" -exec rm {} \;
	find ./unittest -name "*.class" -exec rm {} \;
	rm -rf ./classfiles \;
#	find ./classfiles -name "*.class" -exec rm {} \;

apollo : main gui config adapters ensjadapter syntenyviewer sample org-bdgp \
	org-jdesktop

build-apollo : clean-apollo apollo

clean-apollo : 
	find ./classfiles/apollo -name "*.class" -exec rm {} \;

classdir :
	mkdir -p classfiles

#builds an arbitrary class file
classfiles/%.class	: %.java
	javac $(JFLAGS) $<


mainClasses = $(patsubst %.java, classfiles/%.class, $(wildcard apollo/main/*.java))
main : $(mainClasses)

# got rid of datamodel dependencies on gui (at last!!!)
clean-datamodel :
	find ./classfiles/apollo/datamodel -name "*.class" -exec rm {} \;
build-datamodel : clean-datamodel datamodel

## This kind of thing should eventually be done for whole makefile
datamodelClasses = $(patsubst %.java, classfiles/%.class, $(wildcard apollo/datamodel/*.java))
datamodelSeqClasses = $(patsubst %.java, classfiles/%.class, $(wildcard apollo/datamodel/seq/*.java))
datamodel : $(datamodelClasses) $(datamodelSeqClasses)

editorClasses = $(patsubst %.java, classfiles/%.class, $(wildcard apollo/editor/*.java))
editor : $(editorClasses)

clean-editor :
	find ./classfiles/apollo/editor -name "*.class" -exec rm {} \;
build-editor : clean-editor editor

orgBdgpSource = $(wildcard org/bdgp/*/*.java)
orgBdgpClasses = $(patsubst %.java, classfiles/%.class,$(orgBdgpSource) )
# swing/widget
subOrgBdgpSource = $(wildcard org/bdgp/*/*/*.java)
subOrgBdgpClasses = $(patsubst %.java, classfiles/%.class,$(subOrgBdgpSource) )
org-bdgp : $(orgBdgpClasses) $(subOrgBdgpClasses)

orgJDesktopSource = $(wildcard org/jdesktop/*/*.java)
orgJDesktopClasses = $(patsubst %.java, classfiles/%.class,$(orgJDesktopSource) )
org-jdesktop : $(orgJDesktopClasses)

util	: \
	classfiles/apollo/util/CaseInsensitiveStringHash.class \
	classfiles/apollo/util/ClipboardUtil.class \
	classfiles/apollo/util/Comparator.class \
	classfiles/apollo/util/DateUtil.class \
	classfiles/apollo/util/ExampleFileFilter.class \
	classfiles/apollo/util/FastaHeader.class \
	classfiles/apollo/util/FeatureAlphabeticalComparator.class \
	classfiles/apollo/util/FeatureComparator.class \
	classfiles/apollo/util/FeatureIterator.class \
	classfiles/apollo/util/FeatureList.class \
	classfiles/apollo/util/Format.class \
	classfiles/apollo/util/GuiUtil.class \
	classfiles/apollo/util/HTMLUtil.class \
	classfiles/apollo/util/IOUtil.class \
	classfiles/apollo/util/NumericKeyFilter.class \
	classfiles/apollo/util/QuickSort.class \
	classfiles/apollo/util/SeqFeatureUtil.class \
	classfiles/apollo/util/SequenceUtil.class \
	classfiles/apollo/util/SwingMissingUtil.class \
	classfiles/apollo/util/Visitor.class \
	classfiles/apollo/test/WashingLine.class \

analysis : \
	classfiles/apollo/analysis/filter/AnalysisFilterI.class \
	classfiles/apollo/analysis/filter/AnalysisFilter.class \
	classfiles/apollo/analysis/filter/AnalysisInput.class \
	classfiles/apollo/analysis/filter/Compress.class \
	classfiles/apollo/analysis/filter/Coverage.class \
	classfiles/apollo/analysis/filter/HuffNode.class \
	classfiles/apollo/analysis/filter/TwilightFilter.class \
	classfiles/apollo/dataadapter/analysis/AbstractParser.class \
	classfiles/apollo/dataadapter/analysis/AlignmentParser.class \
	classfiles/apollo/dataadapter/analysis/AnalysisParserI.class \
	classfiles/apollo/dataadapter/analysis/BlastParser.class \
	classfiles/apollo/dataadapter/analysis/BlatParser.class \
	classfiles/apollo/dataadapter/analysis/GeneFinderParser.class \
	classfiles/apollo/dataadapter/analysis/GenscanParser.class \
	classfiles/apollo/dataadapter/analysis/FgeneshParser.class \
	classfiles/apollo/dataadapter/analysis/FgeneshGFFParser.class \
	classfiles/apollo/dataadapter/analysis/GenieParser.class \
	classfiles/apollo/dataadapter/analysis/RpMaskerParser.class \
	classfiles/apollo/dataadapter/analysis/Sim4Parser.class \
	classfiles/apollo/dataadapter/analysis/tRNA_Parser.class \
	classfiles/apollo/seq/io/FastaFile.class \

configSource = $(wildcard apollo/config/*.java)
configClasses = $(patsubst %.java, classfiles/%.class,$(configSource) )
config : $(configClasses)

build-config: clean-config config

clean-config: 
	find ./classfiles/apollo/config -name "*.class" -exec rm {} \;

guiSource = $(wildcard apollo/gui/*.java)
guiClasses = $(patsubst %.java, classfiles/%.class,$(guiSource) )

# annotinfo, drawable, featuretree,event,evidencepanel,genomemap,menus,tweeker
subGuiSource = $(wildcard apollo/gui/*/*.java)
subGuiClasses = $(patsubst %.java, classfiles/%.class,$(subGuiSource) )

# builds gui
gui	: datamodel editor util analysis config exonviewer $(guiClasses) $(subGuiClasses) \
	classfiles/apollo/analysis/AnalysisDataAdapterI.class \
	classfiles/apollo/analysis/filter/FilterPanelI.class \
	classfiles/apollo/analysis/filter/FilterPanel.class \
	classfiles/apollo/analysis/filter/BlastFilterPanel.class \
	classfiles/apollo/analysis/filter/GenscanFilterPanel.class \
	classfiles/apollo/analysis/filter/NoFilterPanel.class \
	classfiles/apollo/analysis/filter/Sim4FilterPanel.class \
	classfiles/apollo/dataadapter/analysis/AnalysisAdapter.class \
	classfiles/apollo/dataadapter/analysis/AnalysisAdapterGUI.class \
	classfiles/apollo/dataadapter/analysis/AnalysisPanel.class \
	classfiles/apollo/analysis/BlastHandler.class \
	classfiles/apollo/analysis/GenericProgramHandler.class \
	classfiles/apollo/analysis/ProgramHandlerFactory.class \
	classfiles/apollo/analysis/ProgramHandlerI.class \
	classfiles/apollo/analysis/SeqAnalysisBDGP.class \
	classfiles/apollo/analysis/SeqAnalysisBase.class \
	classfiles/apollo/analysis/SeqAnalysisFactory.class \
	classfiles/apollo/analysis/SeqAnalysisGUI.class \
	classfiles/apollo/analysis/SeqAnalysisI.class \
	classfiles/apollo/analysis/SeqAnalysisLocal.class \
	classfiles/apollo/analysis/SeqAnalysisNCBI.class \
	classfiles/apollo/analysis/SeqAnalysisRemote.class \
	classfiles/apollo/gui/detailviewers/blixem/BlixemRunner.class \
	classfiles/apollo/gui/detailviewers/seqexport/SeqExport.class \
	classfiles/apollo/io/FileParse.class \
	classfiles/apollo/seq/ResidueProperties.class \
	classfiles/apollo/seq/io/GFFFile.class \
	classfiles/apollo/seq/io/MSPFile.class \
	classfiles/edu/stanford/ejalbert/BrowserLauncher.class 

exonviewerSource = $(wildcard apollo/gui/detailviewers/exonviewer/*.java)
exonviewerClasses = $(patsubst %.java, classfiles/%.class,$(exonviewerSource))

exonviewer: $(exonviewerClasses)

clean-genomemap:
	find ./classfiles/apollo/gui/genomemap -name "*.class" -exec rm {} \;

build-genomemap: clean-genomemap gui

clean-gui:
	find ./classfiles/apollo/gui -name "*.class" -exec rm {} \;

build-gui: clean-gui gui

clean-drawable:
	find ./classfiles/apollo/gui/drawable -name "*.class" -exec rm {} \;

build-drawable: clean-drawable gui

syntenyviewer: gui \
	classfiles/apollo/gui/synteny/CurationManager.class \
	classfiles/apollo/gui/synteny/GuiCurationState.class \
	classfiles/apollo/gui/synteny/CompositeApolloFrame.class \
	classfiles/apollo/gui/synteny/DrawableLink.class \
	classfiles/apollo/gui/synteny/SyntenyLayout.class \
	classfiles/apollo/gui/synteny/SyntenyLinkPanel.class \
	classfiles/apollo/gui/synteny/SyntenyLinkPolygon.class \
	classfiles/apollo/gui/synteny/SyntenyPanel.class \
	classfiles/apollo/dataadapter/synteny/ChromosomeDrawer.class \
	classfiles/apollo/dataadapter/synteny/ChromosomePanel.class \
	classfiles/apollo/dataadapter/synteny/FullEnsJDBCSyntenyPanel.class \
	classfiles/apollo/dataadapter/synteny/SyntenyAdapter.class \
	classfiles/apollo/dataadapter/synteny/SyntenyAdapterGUI.class \
	classfiles/apollo/dataadapter/synteny/SyntenyComparaAdapter.class \
	classfiles/apollo/dataadapter/synteny/AlignBlockChooser.class \
	classfiles/apollo/dataadapter/synteny/builder/BuilderAdapter.class \
	classfiles/apollo/dataadapter/synteny/builder/BuilderAdapterGUI.class \
	classfiles/apollo/dataadapter/synteny/SyntenyComparaAdapterGUI.class \
	classfiles/apollo/dataadapter/synteny/SyntenyComparaAdapterGUI.class \
	classfiles/apollo/dataadapter/synteny/EmbeddedLinkSynteny.class \
	classfiles/apollo/dataadapter/synteny/LinkerI.class \
	classfiles/apollo/dataadapter/synteny/AbstractLinker.class \
	classfiles/apollo/dataadapter/synteny/PeptideLinker.class \
	classfiles/apollo/dataadapter/synteny/SelfLinker.class \
	classfiles/apollo/dataadapter/synteny/TranscriptLinker.class \
	classfiles/apollo/dataadapter/synteny/ResultAnnotPairI.class \
	classfiles/apollo/dataadapter/synteny/ResultTranscriptPair.class \
	classfiles/apollo/dataadapter/synteny/ResultPeptidePair.class \
	classfiles/apollo/dataadapter/synteny/SyntenyMenu.class

clean-synteny:
	find ./classfiles/apollo/dataadapter/synteny -name "*.class" -exec rm {} \;
	find ./classfiles/apollo/gui/synteny -name "*.class" -exec rm {} \;

build-synteny: clean-synteny syntenyviewer

gameadapter: \
	classfiles/apollo/dataadapter/gamexml/XMLParser.class \
	classfiles/apollo/dataadapter/gamexml/GAMEAdapter.class \
#	classfiles/apollo/dataadapter/gamexml/GAMEPanel.class \
	classfiles/apollo/dataadapter/gamexml/GAMESave.class \
	classfiles/apollo/dataadapter/gamexml/LocationGAMEPanel.class \
	classfiles/apollo/dataadapter/gamexml/SequenceGAMEPanel.class \
	classfiles/apollo/dataadapter/gamexml/TransactionXMLAdapter.class \
	classfiles/apollo/dataadapter/gamexml/GAMEAdapterGUI.class

clean-gameadapter:
	find ./classfiles/apollo/dataadapter/gamexml -name "*.class" -exec rm {} \;

build-gameadapter: clean-gameadapter gameadapter

flySource = $(wildcard apollo/dataadapter/fly*/*.java)
flyClasses = $(patsubst %.java, classfiles/%.class,$(flySource))

flyadapter: $(flyClasses)

gbadapter: \
	classfiles/apollo/dataadapter/genbank/FeatureValidatorI.class \
	classfiles/apollo/dataadapter/genbank/FlyIdValidator.class \
	classfiles/apollo/dataadapter/genbank/GenbankAdapter.class \
	classfiles/apollo/dataadapter/genbank/GenbankAdapterGUI.class \
	classfiles/apollo/dataadapter/genbank/GenbankRead.class \
	classfiles/apollo/dataadapter/genbank/GenbankValidator.class \
	classfiles/apollo/dataadapter/genbank/PeptideNote.class \
	classfiles/apollo/dataadapter/genbank/PeptideValidator.class \
	classfiles/apollo/dataadapter/genbank/PublicDbFeature.class \
	classfiles/apollo/dataadapter/genbank/PublicDbPanel.class \
	classfiles/apollo/dataadapter/genbank/tRNAValidator.class \
	classfiles/apollo/dataadapter/genbank/XrefUtil.class \
	classfiles/apollo/dataadapter/genbank/GenbankReport.class \
	classfiles/apollo/dataadapter/genbank/GenbankFormatter.class 

das: \
	classfiles/apollo/dataadapter/das/DASLazySequence.class \
	classfiles/apollo/dataadapter/das/DASDsn.class \
	classfiles/apollo/dataadapter/das/DASSegment.class \
	classfiles/apollo/dataadapter/das/DASSequence.class \
	classfiles/apollo/dataadapter/das/DASServerI.class \
	classfiles/apollo/dataadapter/das/DASFeature.class \
	classfiles/apollo/dataadapter/das/DASClient.class \
	classfiles/apollo/dataadapter/das/DASAdapterGUI.class \
	classfiles/apollo/dataadapter/das/DASAdapter.class

simpledas: \
	classfiles/apollo/dataadapter/das/simple/SimpleDASDsn.class \
	classfiles/apollo/dataadapter/das/simple/SimpleDASSegment.class \
	classfiles/apollo/dataadapter/das/simple/SimpleDASFeature.class \
	classfiles/apollo/dataadapter/das/simple/SimpleDASSequence.class \
	classfiles/apollo/dataadapter/das/simple/SimpleDASServer.class \
	classfiles/apollo/dataadapter/das/simple/DASFeatureContentHandler.class \
	classfiles/apollo/dataadapter/das/simple/DASDsnContentHandler.class \
	classfiles/apollo/dataadapter/das/simple/DASEntryPointContentHandler.class \
	classfiles/apollo/dataadapter/das/simple/DASSequenceContentHandler.class \
	classfiles/apollo/dataadapter/das/simple/SimpleDASAdapterGUI.class \
	classfiles/apollo/dataadapter/das/simple/SimpleDASAdapter.class

das2adapterSource = $(wildcard apollo/dataadapter/das2/*.java)
das2adapterClasses = $(patsubst %.java, classfiles/%.class,$(das2adapterSource))
das2adapter: $(das2adapterClasses)

graphadapterSource = $(wildcard apollo/dataadapter/graph/*.java)
graphadapterClasses = $(patsubst %.java, classfiles/%.class,$(graphadapterSource))
graphadapter: $(graphadapterClasses)


enscgiadapter: \
	classfiles/apollo/dataadapter/EnsCGIAdapter.class \
	classfiles/apollo/dataadapter/EnsCGIAdapterGUI.class

gffadapter: \
	classfiles/apollo/dataadapter/BaseGFFAdapter.class \
	classfiles/apollo/dataadapter/GFFAdapter.class \
	classfiles/apollo/dataadapter/GFFAdapterGUI.class

gff3adapter: \
	classfiles/apollo/dataadapter/gff3/GFF3AdapterGUI.class \
	classfiles/apollo/dataadapter/gff3/GFF3Adapter.class \
	classfiles/apollo/dataadapter/gff3/GFF3Entry.class

sample: \
	classfiles/apollo/dataadapter/sample/SampleAdapter.class \
	classfiles/apollo/dataadapter/sample/SampleAdapterGUI.class

ensjadapter: \
	classfiles/apollo/dataadapter/ensj/NonFatalException.class  \
	classfiles/apollo/dataadapter/ensj/FatalException.class  \
	classfiles/apollo/dataadapter/ensj/EnsJAdapter.class  \
	classfiles/apollo/dataadapter/ensj/EnsJAdapterGUI.class  \
	classfiles/apollo/dataadapter/ensj/DataFetcher.class \
	classfiles/apollo/dataadapter/ensj/model/AnnotationsModel.class \
	classfiles/apollo/dataadapter/ensj/model/DatabaseModel.class \
	classfiles/apollo/dataadapter/ensj/model/LocationModel.class \
	classfiles/apollo/dataadapter/ensj/model/Model.class \
	classfiles/apollo/dataadapter/ensj/model/TypesModel.class \
	classfiles/apollo/dataadapter/ensj/controller/ActionEventRouter.class \
	classfiles/apollo/dataadapter/ensj/controller/ChangeDatabaseHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/ClearSeqRegionsHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/Controller.class \
	classfiles/apollo/dataadapter/ensj/controller/EventHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/EventRouter.class \
	classfiles/apollo/dataadapter/ensj/controller/FindCoordSystemsHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/FindEnsemblDatabaseNamesHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/FindSeqRegionsHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/HideShowAnnotationsHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/HideShowDatabasesHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/HideShowTypesHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/KeyEventRouter.class \
	classfiles/apollo/dataadapter/ensj/controller/LoadSeqRegionByHistoryHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/PopupRouter.class \
	classfiles/apollo/dataadapter/ensj/controller/ReadHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/SelectNewEnsemblDatabaseHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/SelectSeqRegionHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/ShowAbInitioCountsByTypeHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/ShowDnaDnaAlignCountsByTypeHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/ShowDnaProteinAlignCountsByTypeHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/ShowGeneCountsByTypeHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/ShowSimpleFeatureCountsByTypeHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/UpdateHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/UseSeqRegionLocationHandler.class \
	classfiles/apollo/dataadapter/ensj/controller/UseStableIDLocationHandler.class \
	classfiles/apollo/dataadapter/ensj/view/AnnotationsPanel.class \
	classfiles/apollo/dataadapter/ensj/view/DatabasePanel.class \
	classfiles/apollo/dataadapter/ensj/view/LocationPanel.class \
	classfiles/apollo/dataadapter/ensj/view/TypesPanel.class \
	classfiles/apollo/dataadapter/ensj/view/View.class 

ensj19adapter: otter \
	classfiles/apollo/dataadapter/NonFatalDataAdapterException.class  \
	classfiles/apollo/dataadapter/ensj19/AnnotationEnsJAdapter.class \
	classfiles/apollo/dataadapter/ensj19/AnnotationEnsJAdapterGUI.class \
	classfiles/apollo/dataadapter/ensj19/AnnotationSourceChooser.class \
	classfiles/apollo/dataadapter/ensj19/SourceChooserWithHistory.class \
	classfiles/apollo/dataadapter/ensj19/DataSourceConfigurationPanel.class \
	classfiles/apollo/dataadapter/ensj19/EnsJAdapter.class \
	classfiles/apollo/dataadapter/ensj19/EnsJAdapterGUI.class \
	classfiles/apollo/dataadapter/ensj19/AlternateEnsJSequence.class \
	classfiles/apollo/dataadapter/ensj19/AnnotationSourceChooser.class \
	classfiles/apollo/dataadapter/ensj19/EnsJSequence.class \
	classfiles/apollo/dataadapter/ensj19/OtterAnnotationSourceChooser.class

chadoSource = $(wildcard apollo/dataadapter/chado/*.java)
chadoClasses = $(patsubst %.java, classfiles/%.class,$(chadoSource) )



chadoJdbcSource = $(wildcard apollo/dataadapter/chado/jdbc/*.java)
chadoJdbcClasses = $(patsubst %.java, classfiles/%.class,$(chadoJdbcSource) )

chadojdbcadapter: $(chadoJdbcClasses) $(chadoClasses)

clean-chadojdbcadapter :
	find ./classfiles/apollo/dataadapter/chado -name "*.class" -exec rm {} \;

build-chadojdbcadapter: clean-chadojdbcadapter chadojdbcadapter

chadoxmlSource = $(wildcard apollo/dataadapter/chadoxml/*.java)
chadoxmlClasses = $(patsubst %.java, classfiles/%.class,$(chadoxmlSource) )
chadoxmladapter: $(chadoxmlClasses)

serialadapter: \
	classfiles/apollo/dataadapter/SerialAdapterGUI.class \
	classfiles/apollo/dataadapter/SerialDiskAdapter.class

otter: \
	classfiles/apollo/dataadapter/otter/OtterAdapter.class \
	classfiles/apollo/dataadapter/otter/OtterAdapterGUI.class \
	classfiles/apollo/dataadapter/otter/parser/EvidenceNameTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/EvidenceTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/EvidenceTypeTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/ExonEndTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/ExonFrameTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/ExonStableIdTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/ExonStartTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/ExonStrandTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/ExonTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/GenericTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/LocusAuthorEmailTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/LocusAuthorTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/LocusKnownTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/LocusNameTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/LocusRemarkTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/LocusStableIdTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/LocusSynonymTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/LocusTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/LocusTypeTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/OtterContentHandler.class \
	classfiles/apollo/dataadapter/otter/parser/OtterTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/OtterXMLRenderingVisitor.class \
	classfiles/apollo/dataadapter/otter/parser/ResponseTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentAccessionTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentAssemblyEndTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentAssemblyStartTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentAuthorEmailTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentAuthorTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentChromosomeTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentFragmentOffsetTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentFragmentOrientationTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentIdTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentKeywordTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentRemarkTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceFragmentVersionTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceSetAssemblyTypeTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceSetAuthorTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/SequenceSetTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptAuthorEmailTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptAuthorTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptCdsEndNotFoundTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptCdsStartNotFoundTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptClassTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptMRNAEndNotFoundTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptMRNAStartNotFoundTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptNameTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptRemarkTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptStableIdTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptTranslationEndTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptTranslationStableIdTagHandler.class \
	classfiles/apollo/dataadapter/otter/parser/TranscriptTranslationStartTagHandler.class

dataadapterSource =  $(wildcard apollo/dataadapter/*.java)
dataadapterClasses = $(patsubst %.java, classfiles/%.class,$(dataadapterSource))
adapters : $(dataadapterClasses) simpledas gameadapter flyadapter enscgiadapter \
	serialadapter gffadapter gff3adapter gbadapter chadojdbcadapter chadoxmladapter das2adapter graphadapter

build-dataadapter : clean-dataadapter adapters
clean-dataadapter :
	find ./classfiles/apollo/dataadapter -name "*.class" -exec rm {} \;

bop	: datamodel util analysis \
	classfiles/apollo/dataadapter/ApolloAdapterException.class \
	classfiles/apollo/dataadapter/exception/NoOutputException.class \
	classfiles/apollo/dataadapter/DataInputType.class \
	classfiles/apollo/dataadapter/gamexml/GAMESave.class \
	classfiles/apollo/bop/JDBCPipelineAdapter.class \
	classfiles/apollo/bop/JobOutput.class \
	classfiles/apollo/bop/bop.class \

clean-jalview: 
	find ./classfiles/jalview -name "*.class" -exec rm {} \;
build-jalview: clean-jalview jalview
clean-jalview-datamodel:
	find ./classfiles/jalview/datamodel -name "*.class" -exec rm {} \;
build-jalview-datamodel: clean-jalview-datamodel jalview

jalview	: \
	classfiles/jalview/gui/AlignFrame.class \
	classfiles/jalview/analysis/AAFrequency.class \
	classfiles/jalview/analysis/AlignSeq.class \
	classfiles/jalview/analysis/AlignSeqDriver.class \
	classfiles/jalview/analysis/AlignmentSorter.class \
	classfiles/jalview/analysis/AlignmentUtil.class \
	classfiles/jalview/analysis/CGI.class \
	classfiles/jalview/analysis/ClustalwCGI.class \
	classfiles/jalview/analysis/ClustalwThread.class \
	classfiles/jalview/analysis/CommandThread.class \
	classfiles/jalview/analysis/Conservation.class \
	classfiles/jalview/analysis/NJTree.class \
	classfiles/jalview/analysis/PCA.class \
	classfiles/jalview/analysis/PostalCGI.class \
	classfiles/jalview/analysis/blast/BPLite.class \
	classfiles/jalview/analysis/blast/HSP.class \
	classfiles/jalview/analysis/blast/Hit.class \
	classfiles/jalview/analysis/blast/TestRegex.class \
	classfiles/jalview/datamodel/Alignment.class \
	classfiles/jalview/datamodel/CondensedAlignment.class \
	classfiles/jalview/datamodel/Mapper.class \
	classfiles/jalview/datamodel/AlignmentI.class \
	classfiles/jalview/datamodel/BinaryNode.class \
	classfiles/jalview/datamodel/BinarySequence.class \
	classfiles/jalview/datamodel/BlockAlignment.class \
	classfiles/jalview/datamodel/FeatureBlock.class \
	classfiles/jalview/datamodel/Mapper.class \
	classfiles/jalview/datamodel/MSPSequence.class \
	classfiles/jalview/datamodel/ScoreSequence.class \
	classfiles/jalview/datamodel/AlignSequence.class \
	classfiles/jalview/datamodel/SequenceGroup.class \
	classfiles/jalview/datamodel/AlignSequenceI.class \
	classfiles/jalview/datamodel/SequenceNode.class \
	classfiles/jalview/datamodel/SequencePoint.class \
	classfiles/jalview/gui/AlignViewport.class \
	classfiles/jalview/gui/AlignmentOutputGenerator.class \
	classfiles/jalview/gui/AlignmentPanel.class \
	classfiles/jalview/gui/BrowserFrame.class \
	classfiles/jalview/gui/ColorSelectPanel.class \
	classfiles/jalview/gui/ColorSelectPanelDriver.class \
	classfiles/jalview/gui/ColourCanvas.class \
	classfiles/jalview/gui/ColourChooserFrame.class \
	classfiles/jalview/gui/ColourChooserPanel.class \
	classfiles/jalview/gui/ColourPanel.class \
	classfiles/jalview/gui/ColourPanelDriver.class \
	classfiles/jalview/gui/ColumnSelection.class \
	classfiles/jalview/gui/Config.class \
	classfiles/jalview/gui/ControlledCanvas.class \
	classfiles/jalview/gui/ControlledObjectI.class \
	classfiles/jalview/gui/ControlledPanel.class \
	classfiles/jalview/gui/Controller.class \
	classfiles/jalview/gui/ControllerDebugListener.class \
	classfiles/jalview/gui/Debug.class \
	classfiles/jalview/gui/DrawableAlignment.class \
	classfiles/jalview/gui/DrawableSequence.class \
	classfiles/jalview/gui/FeatureFrame.class \
	classfiles/jalview/gui/IdCanvas.class \
	classfiles/jalview/gui/IdPanel.class \
	classfiles/jalview/gui/PCAFrame.class \
	classfiles/jalview/gui/PCAPanel.class \
	classfiles/jalview/gui/PCAThread.class \
	classfiles/jalview/gui/PairAlignFrame.class \
	classfiles/jalview/gui/PairAlignThread.class \
	classfiles/jalview/gui/ProgressFrame.class \
	classfiles/jalview/gui/ProgressPanel.class \
	classfiles/jalview/gui/ProgressPanelDriver.class \
	classfiles/jalview/gui/RotatableCanvas.class \
	classfiles/jalview/gui/Rubberband.class \
	classfiles/jalview/gui/RubberbandRectangle.class \
	classfiles/jalview/gui/ScaleCanvas.class \
	classfiles/jalview/gui/ScalePanel.class \
	classfiles/jalview/gui/ScrollFillPanel.class \
	classfiles/jalview/gui/Selection.class \
	classfiles/jalview/gui/SeqCanvas.class \
	classfiles/jalview/gui/SeqFillPanel.class \
	classfiles/jalview/gui/SeqPanel.class \
	classfiles/jalview/gui/SequenceFeatureThread.class \
	classfiles/jalview/gui/SimpleBrowser.class \
	classfiles/jalview/gui/StatusBar.class \
	classfiles/jalview/gui/TextFrame.class \
	classfiles/jalview/gui/TextFrameDriver.class \
	classfiles/jalview/gui/ThresholdPanel.class \
	classfiles/jalview/gui/ThresholdPanelDriver.class \
	classfiles/jalview/gui/TreeCanvas.class \
	classfiles/jalview/gui/TreeFrame.class \
	classfiles/jalview/gui/TreeFrameDriver.class \
	classfiles/jalview/gui/TreePanel.class \
	classfiles/jalview/gui/TreeThread.class \
	classfiles/jalview/gui/event/AlignViewportEvent.class \
	classfiles/jalview/gui/event/AlignViewportListener.class \
	classfiles/jalview/gui/event/ColumnSelectionEvent.class \
	classfiles/jalview/gui/event/ColumnSelectionListener.class \
	classfiles/jalview/gui/event/EditEvent.class \
	classfiles/jalview/gui/event/EditListener.class \
	classfiles/jalview/gui/event/FontChangeEvent.class \
	classfiles/jalview/gui/event/FontChangeListener.class \
	classfiles/jalview/gui/event/RubberbandEvent.class \
	classfiles/jalview/gui/event/RubberbandListener.class \
	classfiles/jalview/gui/event/SchemeChangeEvent.class \
	classfiles/jalview/gui/event/SchemeChangeListener.class \
	classfiles/jalview/gui/event/SequenceSelectionEvent.class \
	classfiles/jalview/gui/event/SequenceSelectionListener.class \
	classfiles/jalview/gui/event/StatusEvent.class \
	classfiles/jalview/gui/event/StatusListener.class \
	classfiles/jalview/gui/event/TreeSelectionEvent.class \
	classfiles/jalview/gui/event/TreeSelectionListener.class \
	classfiles/jalview/gui/menus/ActionEventSourceI.class \
	classfiles/jalview/gui/menus/AlignMenu.class \
	classfiles/jalview/gui/menus/CalcMenu.class \
	classfiles/jalview/gui/menus/ColourMenu.class \
	classfiles/jalview/gui/menus/CommandAdapter.class \
	classfiles/jalview/gui/menus/CommandLog.class \
	classfiles/jalview/gui/menus/CommandParser.class \
	classfiles/jalview/gui/menus/EditMenu.class \
	classfiles/jalview/gui/menus/FileMenu.class \
	classfiles/jalview/gui/menus/FontMenu.class \
	classfiles/jalview/gui/menus/FrameMenu.class \
	classfiles/jalview/gui/menus/HelpMenu.class \
	classfiles/jalview/gui/menus/JalAction.class \
	classfiles/jalview/gui/menus/JalActionEvent.class \
	classfiles/jalview/gui/menus/JalActionListener.class \
	classfiles/jalview/gui/menus/JalCheckBoxGroup.class \
	classfiles/jalview/gui/menus/JalCheckBoxMenuItem.class \
	classfiles/jalview/gui/menus/JalCommand.class \
	classfiles/jalview/gui/menus/JalCommandVector.class \
	classfiles/jalview/gui/menus/JalMenu.class \
	classfiles/jalview/gui/menus/JalMenuItem.class \
	classfiles/jalview/gui/menus/JalToggleAction.class \
	classfiles/jalview/gui/menus/JalToggleActionGroup.class \
	classfiles/jalview/gui/menus/JalToggleGroup.class \
	classfiles/jalview/gui/menus/JalToggleNotifier.class \
	classfiles/jalview/gui/menus/JalTwoStringMenuItem.class \
	classfiles/jalview/gui/menus/JalTwoStringToggleAction.class \
	classfiles/jalview/gui/menus/MenuManager.class \
	classfiles/jalview/gui/menus/TextCommandAdapter.class \
	classfiles/jalview/gui/menus/ToggleI.class \
	classfiles/jalview/gui/menus/ViewMenu.class \
	classfiles/jalview/gui/popups/AppletFilePopup.class \
	classfiles/jalview/gui/popups/AppletPostscriptPopup.class \
	classfiles/jalview/gui/popups/BrowsePopup.class \
	classfiles/jalview/gui/popups/CommandPopup.class \
	classfiles/jalview/gui/popups/ErrorPopup.class \
	classfiles/jalview/gui/popups/FilePopup.class \
	classfiles/jalview/gui/popups/GroupPopup.class \
	classfiles/jalview/gui/popups/IncrementPopup.class \
	classfiles/jalview/gui/popups/InputPopup.class \
	classfiles/jalview/gui/popups/JalPopupAction.class \
	classfiles/jalview/gui/popups/JalTextActionEvent.class \
	classfiles/jalview/gui/popups/MailPostscriptPopup.class \
	classfiles/jalview/gui/popups/MailTextPopup.class \
	classfiles/jalview/gui/popups/MailTextPopupDriver.class \
	classfiles/jalview/gui/popups/OutputFilePopup.class \
	classfiles/jalview/gui/popups/OutputPopup.class \
	classfiles/jalview/gui/popups/PDBPopup.class \
	classfiles/jalview/gui/popups/PercentIdentityPopup.class \
	classfiles/jalview/gui/popups/Popup.class \
	classfiles/jalview/gui/popups/PopupDriver.class \
	classfiles/jalview/gui/popups/PopupStatusLabel.class \
	classfiles/jalview/gui/popups/PostscriptFilePopup.class \
	classfiles/jalview/gui/popups/PostscriptPopup.class \
	classfiles/jalview/gui/popups/RedundancyPopup.class \
	classfiles/jalview/gui/popups/ThresholdPopup.class \
	classfiles/jalview/gui/popups/URLPopup.class \
	classfiles/jalview/gui/schemes/Blosum62ColourScheme.class \
	classfiles/jalview/gui/schemes/BuriedColourScheme.class \
	classfiles/jalview/gui/schemes/ColourProperties.class \
	classfiles/jalview/gui/schemes/ColourScheme.class \
	classfiles/jalview/gui/schemes/ColourSchemeFactory.class \
	classfiles/jalview/gui/schemes/ColourSchemeI.class \
	classfiles/jalview/gui/schemes/ColourSchemeProperty.class \
	classfiles/jalview/gui/schemes/ColourSchemePropertyVector.class \
	classfiles/jalview/gui/schemes/Consensus.class \
	classfiles/jalview/gui/schemes/ConsensusColour.class \
	classfiles/jalview/gui/schemes/HelixColourScheme.class \
	classfiles/jalview/gui/schemes/HydrophobicColourScheme.class \
	classfiles/jalview/gui/schemes/PIDColourScheme.class \
	classfiles/jalview/gui/schemes/ResidueColourScheme.class \
	classfiles/jalview/gui/schemes/ResidueProperties.class \
	classfiles/jalview/gui/schemes/ScoreColourScheme.class \
	classfiles/jalview/gui/schemes/SecondaryColourScheme.class \
	classfiles/jalview/gui/schemes/StrandColourScheme.class \
	classfiles/jalview/gui/schemes/TaylorColourScheme.class \
	classfiles/jalview/gui/schemes/TurnColourScheme.class \
	classfiles/jalview/gui/schemes/UserColourScheme.class \
	classfiles/jalview/gui/schemes/ZappoColourScheme.class \
	classfiles/jalview/io/AlignFile.class \
	classfiles/jalview/io/BLCFile.class \
	classfiles/jalview/io/ClustalFile.class \
	classfiles/jalview/io/FastaFile.class \
	classfiles/jalview/io/FileParse.class \
	classfiles/jalview/io/FileProperties.class \
	classfiles/jalview/io/FormatAdapter.class \
	classfiles/jalview/io/FormatFactory.class \
	classfiles/jalview/io/FormatFactoryDriver.class \
	classfiles/jalview/io/FormatProperties.class \
	classfiles/jalview/io/FormatProperty.class \
	classfiles/jalview/io/FormatPropertyVector.class \
	classfiles/jalview/io/MSFfile.class \
	classfiles/jalview/io/MSPFile.class \
	classfiles/jalview/io/Mail.class \
	classfiles/jalview/io/MailProperties.class \
	classfiles/jalview/io/OutputGenerator.class \
	classfiles/jalview/io/PIRFile.class \
	classfiles/jalview/io/PfamFile.class \
	classfiles/jalview/io/PostalFile.class \
	classfiles/jalview/io/Postscript.class \
	classfiles/jalview/io/PostscriptProperties.class \
	classfiles/jalview/io/SendFileCGI.class \
	classfiles/jalview/io/SequenceFeatureSourceI.class \
	classfiles/jalview/io/SwissprotFile.class \
	classfiles/jalview/io/TextAreaPrintStream.class \
	classfiles/jalview/io/TreeFile.class \
	classfiles/jalview/math/Matrix.class \
	classfiles/jalview/math/RotatableMatrix.class \
	classfiles/jalview/util/Comparison.class \
	classfiles/jalview/util/ErrorLog.class \
	classfiles/jalview/util/Format.class \
	classfiles/jalview/util/ListenList.class \
	classfiles/jalview/util/QuickSort.class \
	classfiles/jalview/util/WindowUtil.class

externalClasses = $(patsubst %.java, classfiles/%.class, $(wildcard apollo/external/*.java))
external : $(externalClasses)


#builds jar file for distribution
# Note:  for webstart, need to include all the config files in the jar!
# It would be nice if there were a way to do this without listing them all,
# but I couldn't get it to work with a *.
# Note: for webstart, need to sign the jar using jarsigner.
# pushd ../../jars ; jarsigner -keystore apollostore -storepass Apollo -keypass Apollo -signedjar apollo.signed.jar apollo.jar signFiles ; mv apollo.signed.jar ~/toy/htdocs/annot/apollo/jars ; popd
jar	: all bop
	jar -cvmf manifest.txt $(JARDIR)/apollo.jar \
	-C $(APOLLO_ROOT)/data apollosplash.gerry.jpg \
	-C $(APOLLO_ROOT)/data timestamp \
	-C $(APOLLO_ROOT)/conf apollo.cfg \
	-C $(APOLLO_ROOT)/conf game.style \
	-C $(APOLLO_ROOT)/conf fly.style \
	-C $(APOLLO_ROOT)/conf fly.tiers \
	-C $(APOLLO_ROOT)/conf chado.style \
	-C $(APOLLO_ROOT)/conf ensj.style \
	-C $(APOLLO_ROOT)/conf ensj.tiers \
	-C $(APOLLO_ROOT)/conf rice.style \
	-C $(APOLLO_ROOT)/conf rice.tiers \
	-C $(APOLLO_ROOT)/conf gff.style \
	-C $(APOLLO_ROOT)/conf gff3.style \
	-C $(APOLLO_ROOT)/conf genbank.style \
	-C $(APOLLO_ROOT)/conf synteny-game.style \
	-C $(APOLLO_ROOT)/conf synteny-game.tiers \
	-C $(APOLLO_ROOT)/conf chado-adapter.xml \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/main \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/config \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/dataadapter \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/datamodel \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/editor \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/gui \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/io \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/seq \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/util \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/analysis \
	-C $(APOLLO_ROOT)/src/java/classfiles jalview \
	-C $(APOLLO_ROOT)/src/java/classfiles edu/stanford/ejalbert \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/test/WashingLine.class \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/bop \
	-C $(APOLLO_ROOT)/src/java/classfiles apollo/external \
	-C $(APOLLO_ROOT)/src/java/classfiles org/bdgp \
	-C $(APOLLO_ROOT)/src/java/classfiles org/jdesktop

# This is totally out of date!
docs	:
	mkdir -p ${APOLLO_ROOT}/doc/api
	javadoc  -J-Xmx800m -classpath ${APOLLO_ROOT}/src/java/classfiles:${APOLLO_ROOT}/jars/org.bdgp.jar:${APOLLO_ROOT}/jars/org.jdesktop.jar:${CLASSPATH} -sourcepath ./ -d ${APOLLO_ROOT}/doc/api \
		       apollo.main apollo.gui apollo.gui.menus apollo.config \
		       apollo.datamodel apollo.dataadapter apollo.io \
		       apollo.util apollo.seq org.xml.sax com.microstar.xml \
		       apollo.seq.io apollo.dataadapter.debug apollo.external

# These tests currently do not compile - need to rework
#	classfiles/apollo/dataadapter/AdapterTest.class \
#	classfiles/apollo/dataadapter/FrameworkTests.class
test : classfiles/apollo/test/TestApollo.class

run-test : all test
	java -Xmx800m -classpath ${APOLLO_CLASSPATH} -DAPOLLO_ROOT=$(APOLLO_ROOT) junit.ui.TestRunner apollo.test.TestApollo
#	jaco -classpath classfiles:$(CLASSPATH) -Djava.compiler=NONE -DAPOLLO_ROOT=$(APOLLO_ROOT) unittest.framework.TestRunner apollo.dataadapter.FrameworkTests apollo/dataadapter/tests.prefs
#	java -classpath ${APOLLO_ROOT}/src/java/classfiles:${APOLLO_ROOT}/jars/org.bdgp.jar:${APOLLO_ROOT}/jars/jacorb.jar:${APOLLO_ROOT}/jars/junit.jar -DAPOLLO_ROOT=$(APOLLO_ROOT) junit.ui.TestRunner apollo.dataadapter.FrameworkTests apollo/dataadapter/tests.prefs
#	jaco -classpath classfiles:$(CLASSPATH) -Djava.compiler=NONE -DAPOLLO_ROOT=$(APOLLO_ROOT) unittest.framework.TestRunner apollo.dataadapter.FrameworkTests apollo/dataadapter/tests.prefs

guitest: \
	classfiles/apollo/test/Robby.class



reindent:
	find ./apollo  -name "*.java" -exec $(ASTYLE) {} \;

# deletes .java.pre files produced by reindent target
rempre:
	find ./apollo -name "*.java.pre" -exec rm {} \; 

# For making internal release for FlyBase	
private: privatecfg zip restorecfg
internal: private

privatecfg:
	cd ${APOLLO_ROOT}/conf; \
	mv fly.style fly.public.style; \
	cp -p fly.internal.style fly.style

restorecfg:
	cd ${APOLLO_ROOT}/conf; \
	cp -p fly.public.style fly.style

# Zip file is for users
zip:	jar
	cd ${APOLLO_ROOT}/..; \
	rm -f apollo/apollo.zip; \
	zip -r apollo/apollo.zip apollo/jars/*.jar \
	apollo/LICENSE apollo/README \
	apollo/bin/apollo apollo/bin/mac-set-jvm apollo/bin/get-real-path apollo/bin/stag-diff.pl \
	apollo/data/timestamp apollo/data/apollosplash.gerry.jpg apollo/data/images/head-of-apollo.gif apollo/data/restriction_enzymes.dat apollo/data/history.default \
	apollo/data/example.xml apollo/data/tiny.xml apollo/data/chr2.200000-400000.gff apollo/data/chr2.200000-400000.fasta \
	apollo/data/CG16983.chado.xml apollo/data/small.chado apollo/data/tiny.fa apollo/data/tiny.blast.dmel \
	apollo/data/dmel-synteny-example.xml apollo/data/dpse-example.xml \
	apollo/data/gff3 \
	apollo/conf/apollo.cfg apollo/conf/*.style apollo/conf/*tiers \
	apollo/conf/bop.prefs \
	apollo/conf/logging_info_level.conf apollo/conf/mus_musculus_core_9_3_driver.conf \
	apollo/conf/validation.conf \
	apollo/conf/chado-adapter.xml apollo/conf/tigrsybil.style apollo/conf/tigrsybil.tiers \
	apollo/conf/crm.style apollo/conf/crm.tiers apollo/conf/transactionWritebackTemplate_crm.xml \
	apollo/conf/transactionXMLTemplate.xml apollo/conf/transactionXMLTemplate_rice.xml apollo/conf/transactionXMLTemplate_paramecium.xml \
	apollo/conf/paramecium.style apollo/conf/paramecium.tiers apollo/conf/synteny-chado-jdbc-paramecium.style \
	apollo/doc/html/userguide.html apollo/doc/html/tiers.html apollo/doc/html/synteny.html apollo/doc/html/game-synteny.html apollo/doc/html/dataadapter_cookbook.html \
	apollo/doc/html/game.rng.txt apollo/doc/html/images \
	apollo/doc/NOTES apollo/doc/release-1.6.5-notes apollo/doc/bop-todo apollo/doc/chadoxml-notes apollo/doc/webstart-notes \
	apollo/doc/todo apollo/doc/fixed-bugs apollo/doc/synteny-todo apollo/doc/synteny-fixed apollo/doc/synteny-fine-view-spec apollo/doc/test-list \
	apollo/doc/chado/*[a-z] apollo/doc/schema-notes \
	apollo/doc/game.rng apollo/doc/game.xsd

# Tar file is for developers
# Do the tar from APOLLO_ROOT/.. so that the stuff in the tar will all be
# in the apollo directory, so that if someone untars it in their home directory,
# the stuff will all go into an apollo.version directory rather than straight into
# their home dir.
VERSION   := $(shell fgrep "version = " ~/apollo/src/java/apollo/main/Version.java | sed "s/.*version //" | sed "s/ .*//" | sed 's/[\";]//g' )
APOLLO_DIR = apollo.${VERSION}
tar:	jar
	echo APOLLO_DIR = ${APOLLO_DIR}; \
	cd ${APOLLO_ROOT}; \
	rm -f ${APOLLO_DIR}; \
	ln -s ${APOLLO_ROOT} ${APOLLO_DIR}; \
	rm -f ${APOLLO_ROOT}/EXCLUDE; \
	find ${APOLLO_ROOT}/. -name '*[0-9\~\#]' -print | grep -v ensj19 | grep -v das2 | sed "s/..//" > ${APOLLO_ROOT}/EXCLUDE; \
	find ${APOLLO_ROOT}/. -name '.\#*' -print  | sed "s/..//" >> ${APOLLO_ROOT}/EXCLUDE; \
	find ${APOLLO_ROOT}/. -name '*old' -print  | sed "s/..//" >> ${APOLLO_ROOT}/EXCLUDE; \
	find ${APOLLO_ROOT}/. -name '.java.*' -print  | sed "s/..//" >> ${APOLLO_ROOT}/EXCLUDE; \
	find ${APOLLO_ROOT}/. -name '*class' -print  | sed "s/..//" >> ${APOLLO_ROOT}/EXCLUDE; \
	find ${APOLLO_ROOT}/src/java/classfiles/ -print >> ${APOLLO_ROOT}/EXCLUDE; \
	find ${APOLLO_ROOT}/. -name 'CVS' -print  | sed "s/..//" >> ${APOLLO_ROOT}/EXCLUDE; \
	tar cvfX ${APOLLO_ROOT}/apollo.tar ${APOLLO_ROOT}/EXCLUDE \
	${APOLLO_DIR}/bin/apollo ${APOLLO_DIR}/bin/mac-set-jvm ${APOLLO_DIR}/bin/get-real-path ${APOLLO_DIR}/bin/stag-diff.pl ${APOLLO_DIR}/bin/fix-xml.pl ${APOLLO_DIR}/bin/check-roundtrip.pl ${APOLLO_DIR}/bin/bop ${APOLLO_DIR}/bin/cact.jnlp \
	${APOLLO_DIR}/data/timestamp ${APOLLO_DIR}/data/apollosplash.gerry.jpg ${APOLLO_DIR}/data/restriction_enzymes.dat ${APOLLO_DIR}/data/history.default \
	${APOLLO_DIR}/data/example.xml ${APOLLO_DIR}/data/tiny.xml ${APOLLO_DIR}/data/chr2.200000-400000.gff ${APOLLO_DIR}/data/chr2.200000-400000.fasta ${APOLLO_DIR}/data/ENS*gff ${APOLLO_DIR}/data/HUMAN*gff ${APOLLO_DIR}/data/MOUSE*gff \
	${APOLLO_DIR}/data/dmel-synteny-example.xml ${APOLLO_DIR}/data/dpse-example.xml \
	${APOLLO_DIR}/data/CG16983.chado.xml ${APOLLO_DIR}/data/small.chado \
	${APOLLO_DIR}/conf/*[a-z] \
	${APOLLO_DIR}/doc/html/userguide.html ${APOLLO_DIR}/doc/html/synteny.html ${APOLLO_DIR}/doc/html/game-synteny.html ${APOLLO_DIR}/doc/html/dataadapter_cookbook.html ${APOLLO_DIR}/doc/html/images \
	${APOLLO_DIR}/doc/todo ${APOLLO_DIR}/doc/fixed-bugs ${APOLLO_DIR}/doc/synteny-todo ${APOLLO_DIR}/doc/synteny-fixed ${APOLLO_DIR}/doc/bop-todo ${APOLLO_DIR}/doc/chado \
	${APOLLO_DIR}/doc/NOTES ${APOLLO_DIR}/doc/random_developer_notes.txt ${APOLLO_DIR}/doc/release*notes ${APOLLO_DIR}/doc/chadoxml-notes ${APOLLO_DIR}/doc/webstart-notes ${APOLLO_DIR}/doc/small.roundtripped \
	${APOLLO_DIR}/doc/game.rng ${APOLLO_DIR}/doc/game.xsd \
	${APOLLO_DIR}/LICENSE ${APOLLO_DIR}/README \
	${APOLLO_DIR}/jars/*jar \
	${APOLLO_DIR}/src

javadoc:
	mkdir -p ${APOLLO_ROOT}/doc/javadoc; \
	javadoc -J-Xmx800m -private -d ${APOLLO_ROOT}/doc/javadoc \
	-classpath ${APOLLO_CLASSPATH} \
	-sourcepath ${APOLLO_ROOT}/src/java/apollo \
	apollo/*/*java apollo/*/*/*java apollo/*/*/*/*java \
        org/bdgp/*/*java org/bdgp/*/*/*java \
	org/jdesktop/*/*java


#DO NOT REMOVE THIS LINE OR ADD ANYTHING AFTER IT - ADDMAKE DEPENDS ON IT FOR MAKING DEPENDENCIES
