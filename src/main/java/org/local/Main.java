package org.local;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import static org.local.Indexador.bulkIndex;
public class Main {
    public static void anexaArquivosAoIndice(String startDir, String index) throws IOException {
        File dir = new File(startDir);
        File[] files = dir.listFiles();
        ArrayList<Documento> documentos = new ArrayList<>();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory()) { // Checa se file é um diretório
                    anexaArquivosAoIndice(file.getAbsolutePath(), index); // Começa novo loop
                } else {
                    try {
                        String caminho = file.getAbsolutePath();
                        String autor = null;
                        String[] extensoes = {"doc","docx","dotx","xls", "xlsx","pdf","txt","html","xml","odt","rtf","ppt"};
                        for(String extensao : extensoes){
                            if (caminho.endsWith(extensao)){
                                FileInputStream inputstream = new FileInputStream(file);
                                FileInputStream inputstream1 = new FileInputStream(file);
                                String conteudo = extraiConteudo(inputstream);
                                Metadata metadata = extraiMetadata(inputstream1);
                                String[] array = metadata.names();
                                for(String name : array) {
                                    if (Objects.equals(name, "dc:creator")) {
                                        autor = metadata.get(name);
                                    }
                                }
                                Documento documento = new Documento(conteudo, autor, caminho);
                                documentos.add(documento);
                            }
                        }
                    } catch (TikaException | IOException | SAXException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        bulkIndex(documentos, index);
    }
    public static String extraiConteudo(InputStream stream)
            throws IOException, TikaException, SAXException {
        Parser parser = new AutoDetectParser();
        ContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        parser.parse(stream, handler, metadata, context);
        return handler.toString();
    }
    public static Metadata extraiMetadata(InputStream stream)
            throws IOException, SAXException, TikaException {
        Parser parser = new AutoDetectParser();
        ContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        parser.parse(stream, handler, metadata, context);
        return metadata;
    }
    public static ArrayList<String> lerPastasNoArquivo(File arquivos) {
        BufferedReader reader;
        ArrayList<String> pastas = new ArrayList<>();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(arquivos), StandardCharsets.ISO_8859_1));
            String line = reader.readLine();
            System.out.println("Pastas a serem indexadas:");
            while (line != null) {
                System.out.println(line);
                pastas.add(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pastas;
    }
    public static ArrayList<File> lerArquivosDaPasta(File folder) {
        ArrayList<File> arquivos = new ArrayList<>();
        for (File file : folder.listFiles()) {
            if (!file.isDirectory()) {
                System.out.println(file.getName());
                arquivos.add(file);
            } else {
                lerArquivosDaPasta(file);
            }
        }
        return arquivos;
    }
    public static void indexaPastaIndexes() {
        File folder = new File("c:/Indexes"); //Contém pastas com arquivos de index
        ArrayList<File> arquivos = lerArquivosDaPasta(folder);
        for (File arquivo: arquivos){
            try {
                //File caminho = new File(arquivo.getParent()); //Pega caminho sem nome do arquivo
                File nomeSemCaminho = new File(arquivo.getName());
                String index = nomeSemCaminho.toString().substring(0, nomeSemCaminho.toString().lastIndexOf('.'));
                apagaIndex(index); //Apaga indexes para reindexar
                ArrayList<String> pastas = lerPastasNoArquivo(arquivo);
                for (String pasta : pastas) {
                    try {
                        anexaArquivosAoIndice(pasta, index);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static void apagaIndex(String index) throws IOException {
        try {
            URL url = new URL("http://localhost:9200/" + index);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpCon.setRequestMethod("DELETE");
            httpCon.connect();
            httpCon.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {
        long inicio = System.nanoTime();
        indexaPastaIndexes();
        long tempoCorrido = (System.nanoTime() - inicio) / 1_000_000_000;
        System.out.println("Tempo total para indexar: "
                + tempoCorrido / 60 + " minuto(s).");
    }
}