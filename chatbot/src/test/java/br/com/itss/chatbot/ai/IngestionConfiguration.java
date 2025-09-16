package br.com.itss.chatbot.ai;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

@TestConfiguration(proxyBeanMethods = false)
public class IngestionConfiguration {

    // Directory path for ingestion, configurable via application.properties
    @Value("classpath:/docs")
    private Resource resource;
    
    // Custom DocumentReader to handle multiple file types in a directory
    private static class MultiFileDocumentReader implements Supplier<List<Document>> {

        private final String directoryPath;

        public MultiFileDocumentReader(Resource resource) throws IOException {
            directoryPath = resource.getFile().getAbsolutePath();
        }

        @Override
        public List<Document> get() {
            List<Document> allDocuments = new ArrayList<>();
            try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
                paths.filter(Files::isRegularFile)
                     .forEach(filePath -> {
                         Resource resource = new FileSystemResource(filePath.toFile());
                         DocumentReader reader = createReaderForFile(resource, filePath);
                         if (reader != null) {
                             allDocuments.addAll(reader.get());
                         }
                     });
            } catch (IOException e) {
                throw new RuntimeException("Error reading directory: " + directoryPath, e);
            }
            return allDocuments;
        }

        private DocumentReader createReaderForFile(Resource resource, Path filePath) {
            String extension = StringUtils.getFilenameExtension(filePath.toString());
            if (extension == null) {
                return new TikaDocumentReader(resource); // Fallback for no extension
            }
            extension = extension.toLowerCase();

            switch (extension) {
                case "json":
                    // JSON reader with optional keys
                    return new JsonReader(resource, "description", "content");
                case "txt":
                    // Text reader with filename metadata
                    TextReader textReader = new TextReader(resource);
                    textReader.getCustomMetadata().put("filename", filePath.getFileName().toString());
                    textReader.getCustomMetadata().put("language", "text");
                    return textReader;
                case "html":
                case "htm":
                    // HTML reader with basic config
                    JsoupDocumentReaderConfig htmlConfig = JsoupDocumentReaderConfig.builder()
                            .selector("body")
                            .includeLinkUrls(true)
                            .build();
                    return new JsoupDocumentReader(resource, htmlConfig);
                case "md":
                case "markdown":
                    // Markdown reader with code blocks included
                    MarkdownDocumentReaderConfig mdConfig = MarkdownDocumentReaderConfig.builder()
                            .withIncludeCodeBlock(true)
                            .withAdditionalMetadata("filename", filePath.getFileName().toString())
                            .build();
                    return new MarkdownDocumentReader(resource, mdConfig);
                case "pdf":
                    // PDF reader (page-based)
                    PdfDocumentReaderConfig pdfConfig = PdfDocumentReaderConfig.builder()
                            .withPagesPerDocument(1)
                            .build();
                    return new PagePdfDocumentReader(resource, pdfConfig);
                default:
                    // Fallback to Tika for DOCX, PPTX, XLSX, etc.
                    return new TikaDocumentReader(resource);
            }
        }
    }

    @Bean
    ApplicationRunner init(VectorStore vectorStore) {
        return args -> {
            // Reader: Process all supported file types in the directory
            MultiFileDocumentReader multiReader = new MultiFileDocumentReader(resource);

            // Transformer: Split into chunks
            TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(
                800,  // defaultChunkSize
                350,  // minChunkSizeChars
                5,    // minChunkLengthToEmbed
                10000,// maxNumChunks
                true  // keepSeparator
            );

            // Read, transform, and load
            List<Document> documents = multiReader.get();
            List<Document> splitDocuments = tokenTextSplitter.apply(documents);
            vectorStore.add(splitDocuments); // Or vectorStore.write(splitDocuments)
        };
    }
}
