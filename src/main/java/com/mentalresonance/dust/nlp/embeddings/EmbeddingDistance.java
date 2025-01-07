/*
 *
 *  Copyright 2024-2025 Alan Littleford
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package com.mentalresonance.dust.nlp.embeddings;

import java.util.List;

/**
 * Convenient embedding-distance functions
 */
public class EmbeddingDistance {
    /**
     *
     * @param vectorA embedding
     * @param vectorB embedding
     * @return 1 if same direction, 0 if orthogocal -1 is opposate
     */
    public static double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("Vectors must be of the same size.");
        }

        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            magnitudeA += Math.pow(vectorA.get(i), 2);
            magnitudeB += Math.pow(vectorB.get(i), 2);
        }

        magnitudeA = Math.sqrt(magnitudeA);
        magnitudeB = Math.sqrt(magnitudeB);

        if (magnitudeA == 0.0 || magnitudeB == 0.0) {
            throw new IllegalArgumentException("Vector magnitude cannot be zero.");
        }

        return dotProduct / (magnitudeA * magnitudeB);
    }

    /**
     * Convenenience since value is always positive
     * @param vectorA embedding
     * @param vectorB embedding
     * @return 0 if same through 2 which means opposite
     */
    public static double cosineDistance(List<Double> vectorA, List<Double> vectorB) {
        return 1.0 - cosineSimilarity(vectorA, vectorB);
    }
}

