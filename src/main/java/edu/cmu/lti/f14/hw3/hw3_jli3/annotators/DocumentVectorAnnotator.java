package edu.cmu.lti.f14.hw3.hw3_jli3.annotators;

import java.util.*;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.f14.hw3.hw3_jli3.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_jli3.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_jli3.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {
	/**
	 * Class member to store all the words in the document
	 * dic - key: token; value: index in corpus
	 */
	private HashMap<String, Integer> dic;
	
	/**
	 * Initialize the components
	 *  
	 * @param aContext
	 * @throws ResourceInitializationException
	 *
	 */
	public void initialize(UimaContext aContext) throws ResourceInitializationException{
		super.initialize(aContext);
	}
	
	@Override
	/**
	 * generate the term vectors from cas
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		
		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}

	/**
	 * A basic white-space tokenizer, it deliberately does not split on punctuation!
	 *
	 * @param doc input text
	 * @return    a list of tokens.
	 */

	List<String> tokenize0(String doc) {
	  List<String> res = new ArrayList<String>();
	  
	  for (String s: doc.split("\\s+"))
	    res.add(s);
	  return res;
	}

	/**
	 * This function generates the term frequency for a document annotation
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		
		/**
		 * construct a vector of tokens and update the tokenList in CAS
		 */
		/**
		 * use tokenize0 from above 
		 */
		List<String> words = tokenize0(docText);
		//FSList tokenList = Utils.fromCollectionToFSList(jcas, words);
		
		HashMap<String, Integer> countMap = new HashMap<String, Integer>();
		
		for(String s: words){
			
			if (!countMap.containsKey(s)) {
		        countMap.put(s, 1);
		    } else {
		        countMap.put(s, countMap.get(s) + 1);
		    }
		}
		ArrayList<Token> tokenList = new ArrayList<Token>();
		for (String s: countMap.keySet()) {
			Token token = new Token(jcas);
			token.setText(s);
			token.setFrequency(countMap.get(s)); //normalize
			tokenList.add(token);
		}
		FSList tokenFSList = Utils.fromCollectionToFSList(jcas, tokenList);
		doc.setTokenList(tokenFSList);
	}
}
