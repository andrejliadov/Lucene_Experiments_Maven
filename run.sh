#!/bin/bash
mvn clean package

cp -r results target
cp -r index target
cp -r performance target
cp -r corpus target
cp -r trec_eval target

cd target
java -jar maven.lucene.search-0.0.1-SNAPSHOT.jar

cd trec_eval
./trec_eval -m runid -m num_q -m num_ret -m num_rel -m num_rel_ret -m map -m gm_map -m P.5,10,15,20 ../corpus/formattedQrels.txt ../results/ComplexQueryBM25Scoring.txt > ../performance/ComplexQueryBM25ScoringPerformance.txt
./trec_eval -m runid -m num_q -m num_ret -m num_rel -m num_rel_ret -m map -m gm_map -m P.5,10,15,20 ../corpus/formattedQrels.txt ../results/ComplexQueryCustomScoring.txt > ../performance/ComplexQueryCustomScoringPerformance.txt
./trec_eval -m runid -m num_q -m num_ret -m num_rel -m num_rel_ret -m map -m gm_map -m P.5,10,15,20 ../corpus/formattedQrels.txt ../results/ComplexQueryStandardScoring.txt > ../performance/ComplexQueryStandardScoringPerformance.txt
./trec_eval -m runid -m num_q -m num_ret -m num_rel -m num_rel_ret -m map -m gm_map -m P.5,10,15,20 ../corpus/formattedQrels.txt ../results/SimpleQueryBM25Scoring.txt > ../performance/SimpleQueryBM25ScoringPerformance.txt
./trec_eval -m runid -m num_q -m num_ret -m num_rel -m num_rel_ret -m map -m gm_map -m P.5,10,15,20 ../corpus/formattedQrels.txt ../results/SimpleQueryCustomScoring.txt > ../performance/SimpleQueryCustomScoringPerformance.txt
./trec_eval -m runid -m num_q -m num_ret -m num_rel -m num_rel_ret -m map -m gm_map -m P.5,10,15,20 ../corpus/formattedQrels.txt ../results/SimpleQueryStandardScoring.txt > ../performance/SimpleQueryStandardScoringPerformance.txt
./trec_eval -m runid -m num_q -m num_ret -m num_rel -m num_rel_ret -m map -m gm_map -m P.5,10,15,20 ../corpus/formattedQrels.txt ../results/SimpleQueryVSMScoring.txt > ../performance/SimpleQueryVSMScoringPerformance.txt
./trec_eval -m runid -m num_q -m num_ret -m num_rel -m num_rel_ret -m map -m gm_map -m P.5,10,15,20 ../corpus/formattedQrels.txt ../results/SimpleQueryCustomVSMScoring.txt > ../performance/SimpleQueryCustomVSMScoring.txt

cd ../performance
echo " "
echo "Performance of various search systems:"
echo " "
cat ComplexQueryBM25ScoringPerformance.txt
echo " "
echo " "
cat ComplexQueryStandardScoringPerformance.txt
echo " "
echo " "
cat ComplexQueryCustomScoringPerformance.txt
echo " "
echo " "
cat SimpleQueryBM25ScoringPerformance.txt
echo " "
echo " "
cat SimpleQueryStandardScoringPerformance.txt
echo " "
echo " "
cat SimpleQueryCustomScoringPerformance.txt
echo " "
echo " "
cat SimpleQueryVSMScoringPerformance.txt
echo " "
echo " "
cat SimpleQueryCustomVSMScoring.txt
