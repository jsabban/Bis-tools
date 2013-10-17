/**
 * 
 */
package edu.usc.epigenome.uecgatk.bissnp.filters;

import net.sf.samtools.SAMRecord;

import org.broadinstitute.sting.commandline.Argument;
import org.broadinstitute.sting.gatk.filters.ReadFilter;
import org.broadinstitute.sting.utils.BaseUtils;
import org.broadinstitute.sting.utils.sam.GATKSAMRecord;

import edu.usc.epigenome.uecgatk.bissnp.BaseUtilsMore;
import edu.usc.epigenome.uecgatk.bissnp.BisSNPUtils;
import edu.usc.epigenome.uecgatk.bissnp.BisulfiteSAMConstants;

/**
 * @author yaping
 * @contact lyping1986@gmail.com
 * @time Sep 15, 2013 10:48:39 PM
 * 
 */
public class BisulfiteFivePrimeConvReadsFilter extends ReadFilter {
	@Argument(fullName = "minmum_cytosine_converted", shortName = "minConv", doc = "disregard first few cytosines in the reads which may come from uncomplete bisulfite conversion in the first few cytosines of the reads (5'end). Default:1 ", required = false)
	public static short minConv = 1;
	@Argument(fullName = "five_prime_conversion_pattern_to_check", shortName = "patConv5", doc = "define the methylation pattern to check for five prime bisulfite_conversion.For NOME-seq, it should be WCH. Default: C", required = false)
    public static String patConv5 = "C";
	@Argument(fullName = "c_position_in_five_prime_conversion_pattern", shortName = "posCinPatConv5", doc = "define the cytosine position in methylation pattern to check for five prime bisulfite_conversion. Default: 1", required = false)
    public static int posCinPatConv5 = 1;
	
	/* (non-Javadoc)
	 * @see net.sf.picard.filter.SamRecordFilter#filterOut(net.sf.samtools.SAMRecord)
	 */
	@Override
	public boolean filterOut(SAMRecord read) {
		if(read.getStringAttribute(BisulfiteSAMConstants.MD_TAG) == null){
			return false;
		}
		try {
			tagBisulfiteConvStart((GATKSAMRecord)read);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public static void tagBisulfiteConvStart(GATKSAMRecord samRecord) throws Exception{
		int readLength = samRecord.getReadLength();


		byte[] bases = samRecord.getReadBases();

		
		
		//TODO: need to check window length in boundary...
		byte[] refBases = BisSNPUtils.refStrFromMd(samRecord);
		if (refBases.length <= 0){
			//samRecord.setTemporaryAttribute(BisulfiteSAMConstants.BIT_WISE_TAG, -1);
			return ;
		}
			
		
		boolean negStrand = samRecord.getReadNegativeStrandFlag();
		if(samRecord.getReadPairedFlag() && samRecord.getSecondOfPairFlag())
			negStrand = !negStrand;
		
		if(negStrand){
			bases = BaseUtils.simpleReverseComplement(bases);
			refBases = BaseUtils.simpleReverseComplement(refBases);	
		}
		byte[] patterns = patConv5.getBytes();
		
		if(readLength < patterns.length ){
			//samRecord.setTemporaryAttribute(BisulfiteSAMConstants.BIT_WISE_TAG, -1);
			return ;
		}
		
		short convertedCount = 0;
		for(int i = 0; i <= readLength-patterns.length; i++){
			short numMatchesInRef=0;
			short numMatchesInReads=0;
			boolean conv = false;
			for(int j = i, index = 0; index < patterns.length; index++, j++){
	
				if(BaseUtilsMore.iupacCodeEqualNotConsiderMethyStatus(patterns[index], refBases[j])){
					numMatchesInRef++;
						
				}
				if(BaseUtilsMore.iupacCodeEqualNotConsiderMethyStatus(patterns[index], bases[j])){
					numMatchesInReads++;
				}
				if(index == (posCinPatConv5-1) && BaseUtils.basesAreEqual(bases[j], BaseUtils.T)){
					conv=true;
				}
				
			}
			if(numMatchesInReads == patterns.length-1){
				if( numMatchesInRef == (patterns.length)){
					if(conv)
						convertedCount++;
					
					if(convertedCount >= minConv && minConv>0){
						synchronized(samRecord){
							if(!samRecord.containsTemporaryAttribute(BisulfiteSAMConstants.BIT_WISE_TAG))
								samRecord.setTemporaryAttribute(BisulfiteSAMConstants.BIT_WISE_TAG, i);
						}
						
						//System.err.println(samRecord.getReadName() + "\t" + i + "\t" + samRecord.getReadNegativeStrandFlag() + "\t" + samRecord.getSecondOfPairFlag());
						return ;
					}
				}
			}
			
		}
		//samRecord.setTemporaryAttribute(BisulfiteSAMConstants.BIT_WISE_TAG, -1);
		//if(samRecord.containsTemporaryAttribute(BisulfiteSAMConstants.BIT_WISE_TAG)){
			//System.err.println(samRecord.getReadName() + "\t" + samRecord.getTemporaryAttribute(BisulfiteSAMConstants.BIT_WISE_TAG) + "\t" + samRecord.getReadNegativeStrandFlag() + "\t" + samRecord.getSecondOfPairFlag());
		//}
		
		return;
	}

}
