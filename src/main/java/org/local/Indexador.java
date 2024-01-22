package org.local;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.util.MissingRequiredPropertyException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
/**
 * Cliente para operação de index
 *
 */
public class Indexador {
    private final ElasticsearchClientConfig elasticsearchClientConfig;
    private final ElasticsearchClient elasticsearchClient;

    public Indexador(){
        elasticsearchClientConfig = new ElasticsearchClientConfig();
        this.elasticsearchClient = elasticsearchClientConfig.getElasticsearchClient();
    }
    /**
     * Cria índice utilizando bog-standard ElasticsearchIndicesClient
     *
     */
    public void createIndexUsingClient(String indexName) throws IOException {
        ElasticsearchIndicesClient elasticsearchIndicesClient =
                this.elasticsearchClient.indices();
        CreateIndexRequest createIndexRequest =
                new CreateIndexRequest.Builder().index(indexName).build();
        CreateIndexResponse createIndexResponse =
                elasticsearchIndicesClient.create(createIndexRequest);
        System.out.println("Index utilizando cliente criado com sucesso"+createIndexResponse);
    }
    /**
     * Utilizando expressão Lambda
     * Documento que será indexado
     */
    public void indexDocument(String indexName, Documento documento) throws IOException {
        IndexResponse indexResponse = this.elasticsearchClient.index(
                i -> i.index(indexName)
                        .document(documento)
        );
        System.out.println("Documento indexado com sucesso"+indexResponse);
    }
    public void indexDocumentWithJSON(String indexName, String fileName) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        IndexResponse indexResponse = this.elasticsearchClient.index(
                i -> i.index(indexName)
                        .withJson(inputStream)
        );
        System.out.println("JSON indexado com sucesso"+indexResponse);
    }
    public void close() throws IOException {
        elasticsearchClientConfig.close();
    }
    public static void bulkIndex(ArrayList<Documento> documentos, String index) throws IOException {
        Indexador main = new Indexador();
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();

            for (Documento documento : documentos) {

                br.operations(op -> op                //<1>
                        .index(idx -> idx            //<2>
                                .index(index)       //<3>
                                .document(documento)
                        )
                );
            }
            BulkResponse result = main.elasticsearchClient.bulk(br.build());
        } catch (MissingRequiredPropertyException exception) {
            System.out.println("Request.Builder não possui operações.");
        }
        main.close();
    }
}