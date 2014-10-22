package edu.cmu.lti.f14.hw3.hw3_jli3.casconsumers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_jli3.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_jli3.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_jli3.utils.Utils;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<Integer> relList;

	/** document sentence content */
	public ArrayList<String> txtList;

	/** token - frequency pairs */
	public List<Map<String, Integer>> tokenListArray;

	/** qid - answer indices in the list */
	public Map<Integer, List<Integer>> qaMap;

	/** qid - query index in the list */
	public Map<Integer, Integer> queryMap;

	/** store the cosine similarity for each answer */
	public Map<Integer, Double> answerScoreMap;

	/** store the indices for the correct answer, qid-correctAnswerIndex pairs */
	public Map<Integer, ArrayList<Integer>> correctAnswerMap;

	/** store the indices for the answers for each query, index-score pairs */
	public Map<Integer, Integer> rankMap;

	/** write the output */
	public BufferedWriter writer;

	/** Initialize the variables and writer */
	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();

		relList = new ArrayList<Integer>();

		txtList = new ArrayList<String>();

		tokenListArray = new ArrayList<Map<String, Integer>>();

		qaMap = new HashMap<Integer, List<Integer>>();

		queryMap = new HashMap<Integer, Integer>();

		correctAnswerMap = new HashMap<Integer, ArrayList<Integer>>();

		answerScoreMap = new HashMap<Integer, Double>();

		rankMap = new HashMap<Integer, Integer>();

		try {
			writer = new BufferedWriter(new FileWriter(new File("report.txt"),
					false));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 1. construct the global word dictionary 2. keep the word frequency for
	 * each sentence
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas = aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

		if (it.hasNext()) {
			Document doc = (Document) it.next();
			// System.out.println("!");

			// Make sure that your previous annotators have populated this in
			// CAS

			qIdList.add(doc.getQueryID());

			relList.add(doc.getRelevanceValue());
			txtList.add(doc.getText());

			FSList tokenList = doc.getTokenList();
			List<Token> tList = Utils.fromFSListToCollection(tokenList,
					Token.class);

			Map<String, Integer> dic = new HashMap<String, Integer>();
			for (int i = 0; i < tList.size(); i++) {
				dic.put(tList.get(i).getText(), tList.get(i).getFrequency());
			}
			tokenListArray.add(dic);
		}
	}

	/**
	 * 1. Compute Cosine Similarity and rank the retrieved sentences 2.Compute
	 * the MRR metric
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);
		// queryMap : qid: query index in qidList
		// qaMap: qid: answer index in qidList
		for (int i = 0; i < qIdList.size(); i++) {
			int rel = relList.get(i);
			if (rel == 99) {
				Integer key = qIdList.get(i);
				if (!qaMap.containsKey(key)) {
					qaMap.put(key, new ArrayList<Integer>());
					queryMap.put(key, i);
					correctAnswerMap.put(key, new ArrayList<Integer>());
				}
			}
		}

		for (int i = 0; i < qIdList.size(); i++) {
			int rel = relList.get(i);
			if (rel != 99) {
				Integer key = qIdList.get(i);
				qaMap.get(key).add(i);
				if (rel == 1) {
					correctAnswerMap.get(key).add(i);
				}
			}
		}
		/**
		 * compute the cosine similarity measure
		 */
		for (Integer id : queryMap.keySet()) {
			Integer queryIndex = queryMap.get(id);
			ArrayList<Integer> answerIndices = (ArrayList<Integer>) qaMap
					.get(id);

			Map<String, Integer> queryVector = tokenListArray.get(queryIndex);

			for (Integer answerIndex : answerIndices) {
				Map<String, Integer> docVector = tokenListArray
						.get(answerIndex);
				answerScoreMap.put(answerIndex,
						computeCosineSimilarity(queryVector, docVector));
			}
		}

		/**
		 * compute the rank of retrieved sentences and write to the output
		 */
		for (int id = 1; id <= 20; id++) { // the keyset content of the queryId
			// Integer queryIndex = queryMap.get(id);
			ArrayList<Integer> answerIndices = (ArrayList<Integer>) qaMap
					.get(id);
			ArrayList<Integer> correctIndex = correctAnswerMap.get(id);

			for (int i = 0; i < correctIndex.size(); i++) {
				int correct = correctIndex.get(i);
				int rank = 1;
				for (int j = 0; j < answerIndices.size(); j++) {
					int answer = answerIndices.get(j);

					if (correct != answer) {
						if (answerScoreMap.get(correct) < answerScoreMap
								.get(answer))
							rank++;
					}
				}
				rankMap.put(id, rank);

			}
			DecimalFormat df = new DecimalFormat("0.0000");
			String confidence = df.format(answerScoreMap.get(correctIndex
					.get(0)));
			writer.write("cosine=" + confidence + "\t" + "rank="
					+ rankMap.get(id) + "\t" + "qid=" + id + "\t" + "rel=1\t"
					+ txtList.get(correctIndex.get(0)) + "\n");

		}

		/**
		 * compute the mean reciprocal rank and write to the output
		 */
		double metric_mrr = compute_mrr();
		DecimalFormat df = new DecimalFormat("0.0000");
		String confidence = df.format(metric_mrr);
		writer.write("MRR=" + confidence);
		writer.close();
	}

	/**
	 * Calculate the cosine similarity of two Token Lists. The dimensions are
	 * the words and the value of each dimension is their corresponding
	 * frequency.
	 * 
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		double cosine_similarity = 0.0;

		// TODO :: compute cosine similarity between two sentences
		double queryLength = 0;

		double docLength = 0;

		double dotProduct = 0;

		for (String s : queryVector.keySet()) {
			queryLength += queryVector.get(s) * queryVector.get(s);
		}
		queryLength = Math.sqrt(queryLength);

		for (String s : docVector.keySet()) {
			docLength += docVector.get(s) * docVector.get(s);
		}

		docLength = Math.sqrt(docLength);

		for (String s : queryVector.keySet()) {
			if (docVector.containsKey(s)) {
				dotProduct += queryVector.get(s) * docVector.get(s);
			}
		}

		cosine_similarity = dotProduct / (queryLength * docLength);

		return cosine_similarity;
	}

	/**
	 * This function compute the Mean Reciprocal Rank (MRR) of the text
	 * collection
	 * 
	 * @return mrr
	 */
	private double compute_mrr() {
		double metric_mrr = 0.0;

		// TODO :: compute Mean Reciprocal Rank (MRR) of the text collection

		for (Integer id : rankMap.keySet()) {
			metric_mrr += 1.0 / rankMap.get(id);
		}

		if (rankMap.size() > 0)
			metric_mrr /= rankMap.size();

		return metric_mrr;
	}

}
