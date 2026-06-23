package com.drew.metadata;

import com.drew.imaging.ImageMetadataReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class WebMetadataServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Root handler: Serves the Web Dashboard
        server.createContext("/", new IndexHandler());
        
        // API handler: Handles file upload & metadata extraction
        server.createContext("/api/upload", new UploadHandler());

        server.setExecutor(null); // default executor
        System.out.println("=================================================");
        System.out.println("  Metadata Extractor Web Server Started!         ");
        System.out.println("  Access the dashboard at: http://localhost:" + port + " ");
        System.out.println("  Press Ctrl+C to stop the server.               ");
        System.out.println("=================================================");
        server.start();
    }

    static class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            
            String html = getDashboardHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS preflight options
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            try {
                // Read input stream
                InputStream is = exchange.getRequestBody();
                
                // Parse metadata in-memory using our library
                Metadata metadata = ImageMetadataReader.readMetadata(is);
                
                // Serialize metadata to JSON
                String jsonResponse = serializeMetadataToJson(metadata);
                byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
                
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                String errorJson = "{\"error\":" + quote(e.getMessage() != null ? e.getMessage() : e.toString()) + "}";
                byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    private static String serializeMetadataToJson(Metadata metadata) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"directories\":[");
        boolean firstDir = true;
        for (Directory directory : metadata.getDirectories()) {
            if (!firstDir) json.append(",");
            firstDir = false;
            json.append("{");
            json.append("\"name\":").append(quote(directory.getName())).append(",");
            json.append("\"tags\":[");
            boolean firstTag = true;
            for (Tag tag : directory.getTags()) {
                if (!firstTag) json.append(",");
                firstTag = false;
                json.append("{");
                json.append("\"name\":").append(quote(tag.getTagName())).append(",");
                json.append("\"value\":").append(quote(tag.getDescription()));
                json.append("}");
            }
            json.append("],");
            json.append("\"errors\":[");
            boolean firstErr = true;
            for (String error : directory.getErrors()) {
                if (!firstErr) json.append(",");
                firstErr = false;
                json.append(quote(error));
            }
            json.append("]");
            json.append("}");
        }
        json.append("]");
        json.append("}");
        return json.toString();
    }

    private static String quote(String string) {
        if (string == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String getDashboardHtml() {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Metadata Extractor Dashboard</title>
            <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
            <style>
                :root {
                    --bg-dark: #0b0f19;
                    --panel-dark: rgba(20, 26, 40, 0.6);
                    --accent-blue: #4facfe;
                    --accent-cyan: #00f2fe;
                    --text-primary: #f3f4f6;
                    --text-secondary: #9ca3af;
                    --border-color: rgba(255, 255, 255, 0.08);
                    --success-glow: #10b981;
                }

                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }

                body {
                    font-family: 'Inter', sans-serif;
                    background-color: var(--bg-dark);
                    background-image: 
                        radial-gradient(at 0% 0%, rgba(79, 172, 254, 0.08) 0px, transparent 50%),
                        radial-gradient(at 100% 100%, rgba(0, 242, 254, 0.06) 0px, transparent 50%);
                    color: var(--text-primary);
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: flex-start;
                    overflow-x: hidden;
                }

                header {
                    width: 100%;
                    max-width: 1200px;
                    padding: 2rem 1.5rem;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    border-bottom: 1px solid var(--border-color);
                }

                .brand-container {
                    display: flex;
                    align-items: center;
                    gap: 0.75rem;
                }

                .logo-circle {
                    width: 40px;
                    height: 40px;
                    border-radius: 50%;
                    background: linear-gradient(135deg, var(--accent-blue), var(--accent-cyan));
                    box-shadow: 0 0 15px rgba(0, 242, 254, 0.4);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-weight: 700;
                    color: var(--bg-dark);
                    font-size: 1.25rem;
                }

                .brand-title {
                    font-size: 1.5rem;
                    font-weight: 700;
                    background: linear-gradient(135deg, #fff, #a1a1aa);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                }

                .server-status {
                    display: flex;
                    align-items: center;
                    gap: 0.5rem;
                    background: rgba(16, 185, 129, 0.1);
                    border: 1px solid rgba(16, 185, 129, 0.2);
                    padding: 0.5rem 1rem;
                    border-radius: 50px;
                    font-size: 0.85rem;
                    font-weight: 500;
                    color: var(--success-glow);
                }

                .status-dot {
                    width: 8px;
                    height: 8px;
                    background-color: var(--success-glow);
                    border-radius: 50%;
                    box-shadow: 0 0 8px var(--success-glow);
                    animation: pulse 2s infinite;
                }

                @keyframes pulse {
                    0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
                    70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
                    100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
                }

                main {
                    width: 100%;
                    max-width: 1200px;
                    padding: 3rem 1.5rem;
                    flex-grow: 1;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                }

                /* Drag and Drop Zone */
                .dropzone-container {
                    width: 100%;
                    max-width: 650px;
                    background: var(--panel-dark);
                    backdrop-filter: blur(12px);
                    border: 2px dashed rgba(255, 255, 255, 0.15);
                    border-radius: 20px;
                    padding: 4rem 2rem;
                    text-align: center;
                    cursor: pointer;
                    transition: all 0.3s ease;
                    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
                }

                .dropzone-container:hover, .dropzone-container.dragover {
                    border-color: var(--accent-cyan);
                    box-shadow: 0 0 25px rgba(0, 242, 254, 0.15), 0 20px 40px rgba(0, 0, 0, 0.3);
                    transform: translateY(-2px);
                }

                .dropzone-icon {
                    width: 80px;
                    height: 80px;
                    margin: 0 auto 1.5rem;
                    background: rgba(255, 255, 255, 0.03);
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border: 1px solid rgba(255, 255, 255, 0.05);
                    color: var(--accent-blue);
                }

                .dropzone-container:hover .dropzone-icon {
                    color: var(--accent-cyan);
                    background: rgba(0, 242, 254, 0.05);
                }

                .dropzone-title {
                    font-size: 1.25rem;
                    font-weight: 600;
                    margin-bottom: 0.5rem;
                }

                .dropzone-desc {
                    font-size: 0.9rem;
                    color: var(--text-secondary);
                    margin-bottom: 1.5rem;
                }

                .browse-btn {
                    background: linear-gradient(135deg, var(--accent-blue), var(--accent-cyan));
                    color: var(--bg-dark);
                    border: none;
                    padding: 0.75rem 2rem;
                    font-weight: 600;
                    border-radius: 10px;
                    font-size: 0.95rem;
                    cursor: pointer;
                    transition: all 0.2s ease;
                    box-shadow: 0 4px 15px rgba(0, 242, 254, 0.2);
                }

                .browse-btn:hover {
                    opacity: 0.95;
                    box-shadow: 0 4px 20px rgba(0, 242, 254, 0.35);
                    transform: scale(1.02);
                }

                #file-input {
                    display: none;
                }

                /* Loader */
                .loader-container {
                    display: none;
                    flex-direction: column;
                    align-items: center;
                    gap: 1rem;
                }

                .spinner {
                    width: 50px;
                    height: 50px;
                    border: 3px solid rgba(255, 255, 255, 0.05);
                    border-radius: 50%;
                    border-top-color: var(--accent-cyan);
                    animation: spin 1s ease-in-out infinite;
                }

                @keyframes spin {
                    to { transform: rotate(360deg); }
                }

                /* Dashboard UI */
                .dashboard-container {
                    display: none;
                    width: 100%;
                    animation: fadeIn 0.5s ease-out forwards;
                }

                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }

                .dashboard-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 2rem;
                    flex-wrap: wrap;
                    gap: 1rem;
                }

                .file-info {
                    display: flex;
                    flex-direction: column;
                    gap: 0.25rem;
                }

                .file-name {
                    font-size: 1.5rem;
                    font-weight: 700;
                    max-width: 500px;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .file-meta {
                    font-size: 0.9rem;
                    color: var(--text-secondary);
                }

                .reset-btn {
                    background: rgba(255, 255, 255, 0.05);
                    color: var(--text-primary);
                    border: 1px solid var(--border-color);
                    padding: 0.6rem 1.5rem;
                    font-weight: 500;
                    border-radius: 8px;
                    cursor: pointer;
                    transition: all 0.2s ease;
                    display: flex;
                    align-items: center;
                    gap: 0.5rem;
                }

                .reset-btn:hover {
                    background: rgba(255, 255, 255, 0.1);
                    border-color: rgba(255, 255, 255, 0.2);
                }

                /* Quick Cards */
                .summary-cards {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                    gap: 1.5rem;
                    margin-bottom: 2.5rem;
                    width: 100%;
                }

                .summary-card {
                    background: var(--panel-dark);
                    backdrop-filter: blur(12px);
                    border: 1px solid var(--border-color);
                    padding: 1.5rem;
                    border-radius: 16px;
                    display: flex;
                    flex-direction: column;
                    gap: 0.5rem;
                    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
                }

                .card-title {
                    font-size: 0.8rem;
                    font-weight: 600;
                    text-transform: uppercase;
                    color: var(--text-secondary);
                    letter-spacing: 0.05em;
                }

                .card-value {
                    font-size: 1.25rem;
                    font-weight: 700;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .card-subtext {
                    font-size: 0.8rem;
                    color: var(--text-secondary);
                }

                .map-link {
                    color: var(--accent-cyan);
                    text-decoration: none;
                    font-weight: 600;
                    display: inline-flex;
                    align-items: center;
                    gap: 0.25rem;
                    margin-top: 0.25rem;
                }

                .map-link:hover {
                    text-decoration: underline;
                }

                /* Layout */
                .explorer-layout {
                    display: grid;
                    grid-template-columns: 280px 1fr;
                    gap: 2rem;
                    align-items: start;
                }

                @media (max-width: 900px) {
                    .explorer-layout {
                        grid-template-columns: 1fr;
                    }
                }

                .sidebar {
                    background: var(--panel-dark);
                    border: 1px solid var(--border-color);
                    border-radius: 16px;
                    padding: 1rem;
                    max-height: 600px;
                    overflow-y: auto;
                }

                .sidebar-title {
                    font-size: 0.9rem;
                    font-weight: 700;
                    padding: 0.5rem 0.75rem 1rem;
                    border-bottom: 1px solid var(--border-color);
                    margin-bottom: 0.5rem;
                    text-transform: uppercase;
                    letter-spacing: 0.05em;
                    color: var(--text-secondary);
                }

                .directory-btn {
                    width: 100%;
                    background: transparent;
                    border: none;
                    text-align: left;
                    color: var(--text-secondary);
                    padding: 0.75rem 1rem;
                    border-radius: 10px;
                    font-size: 0.9rem;
                    font-weight: 500;
                    cursor: pointer;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    transition: all 0.2s ease;
                }

                .directory-btn:hover {
                    background: rgba(255, 255, 255, 0.03);
                    color: var(--text-primary);
                }

                .directory-btn.active {
                    background: rgba(79, 172, 254, 0.1);
                    color: var(--accent-cyan);
                    border: 1px solid rgba(0, 242, 254, 0.15);
                    font-weight: 600;
                }

                .tag-count {
                    font-size: 0.75rem;
                    background: rgba(255, 255, 255, 0.06);
                    padding: 0.2rem 0.5rem;
                    border-radius: 6px;
                    color: var(--text-secondary);
                }

                .directory-btn.active .tag-count {
                    background: rgba(0, 242, 254, 0.2);
                    color: var(--accent-cyan);
                }

                /* Content Area */
                .details-panel {
                    background: var(--panel-dark);
                    border: 1px solid var(--border-color);
                    border-radius: 16px;
                    padding: 1.5rem;
                    min-height: 400px;
                    display: flex;
                    flex-direction: column;
                    gap: 1.5rem;
                }

                .panel-controls {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    gap: 1rem;
                    flex-wrap: wrap;
                }

                .panel-title {
                    font-size: 1.25rem;
                    font-weight: 700;
                }

                .search-input-container {
                    position: relative;
                    width: 100%;
                    max-width: 300px;
                }

                .search-input {
                    width: 100%;
                    background: rgba(255, 255, 255, 0.04);
                    border: 1px solid var(--border-color);
                    padding: 0.6rem 1rem;
                    border-radius: 8px;
                    color: var(--text-primary);
                    font-size: 0.9rem;
                    transition: all 0.2s ease;
                }

                .search-input:focus {
                    outline: none;
                    border-color: var(--accent-cyan);
                    background: rgba(255, 255, 255, 0.08);
                    box-shadow: 0 0 10px rgba(0, 242, 254, 0.1);
                }

                /* Table styles */
                .table-container {
                    overflow-x: auto;
                    width: 100%;
                    flex-grow: 1;
                }

                table {
                    width: 100%;
                    border-collapse: collapse;
                    text-align: left;
                    font-size: 0.9rem;
                }

                th, td {
                    padding: 1rem;
                    border-bottom: 1px solid var(--border-color);
                }

                th {
                    font-weight: 600;
                    color: var(--text-secondary);
                    background: rgba(255, 255, 255, 0.02);
                }

                tr:hover td {
                    background: rgba(255, 255, 255, 0.01);
                }

                .tag-name-col {
                    font-weight: 500;
                    color: var(--accent-blue);
                    width: 35%;
                }

                .tag-val-col {
                    color: var(--text-primary);
                    word-break: break-all;
                }

                .no-tags {
                    padding: 3rem;
                    text-align: center;
                    color: var(--text-secondary);
                }

                /* Footer */
                footer {
                    width: 100%;
                    padding: 2rem;
                    text-align: center;
                    border-top: 1px solid var(--border-color);
                    color: var(--text-secondary);
                    font-size: 0.85rem;
                    margin-top: auto;
                }
            </style>
        </head>
        <body>
            <header>
                <div class="brand-container">
                    <div class="logo-circle">M</div>
                    <h1 class="brand-title">Metadata Extractor</h1>
                </div>
                <div class="server-status">
                    <div class="status-dot"></div>
                    Server: Active
                </div>
            </header>

            <main>
                <!-- Drag and Drop Panel -->
                <div class="dropzone-container" id="dropzone">
                    <div class="dropzone-icon">
                        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                    </div>
                    <h2 class="dropzone-title">Upload a Media File</h2>
                    <p class="dropzone-desc">Drag and drop your image, video, or audio file here to extract metadata</p>
                    <button class="browse-btn" onclick="document.getElementById('file-input').click()">Browse Files</button>
                    <input type="file" id="file-input" />
                </div>

                <!-- Loader -->
                <div class="loader-container" id="loader">
                    <div class="spinner"></div>
                    <p id="loader-text">Reading file details...</p>
                </div>

                <!-- Dashboard Explorer -->
                <div class="dashboard-container" id="dashboard">
                    <div class="dashboard-header">
                        <div class="file-info">
                            <h2 class="file-name" id="display-filename">image.jpg</h2>
                            <p class="file-meta" id="display-filesize">0 bytes</p>
                        </div>
                        <button class="reset-btn" id="upload-another-btn">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/></svg>
                            Upload Another
                        </button>
                    </div>

                    <!-- Summary Cards -->
                    <div class="summary-cards">
                        <div class="summary-card">
                            <div class="card-title">Camera / Device</div>
                            <div class="card-value" id="summary-camera">-</div>
                            <div class="card-subtext" id="summary-software">-</div>
                        </div>
                        <div class="summary-card">
                            <div class="card-title">Date Taken</div>
                            <div class="card-value" id="summary-date">-</div>
                            <div class="card-subtext">From Exif metadata</div>
                        </div>
                        <div class="summary-card">
                            <div class="card-title">Dimensions</div>
                            <div class="card-value" id="summary-resolution">-</div>
                            <div class="card-subtext" id="summary-filetype">JPEG Image</div>
                        </div>
                        <div class="summary-card">
                            <div class="card-title">GPS Location</div>
                            <div class="card-value" id="summary-gps">-</div>
                            <div class="card-subtext" id="summary-gps-link">No GPS coordinates</div>
                        </div>
                    </div>

                    <!-- Explorer Layout -->
                    <div class="explorer-layout">
                        <div class="sidebar">
                            <h3 class="sidebar-title">Metadata Groups</h3>
                            <div id="directory-list">
                                <!-- Dynamic directory list buttons -->
                            </div>
                        </div>
                        <div class="details-panel">
                            <div class="panel-controls">
                                <h3 class="panel-title" id="active-directory-title">Exif Tags</h3>
                                <div class="search-input-container">
                                    <input type="text" class="search-input" id="tag-search" placeholder="Search tags..." />
                                </div>
                            </div>
                            <div class="table-container">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Tag Name</th>
                                            <th>Value</th>
                                        </tr>
                                    </thead>
                                    <tbody id="tag-table-body">
                                        <!-- Dynamic tags -->
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </main>

            <footer>
                <p>Powered by metadata-extractor library for Java</p>
            </footer>

            <script>
                const dropzone = document.getElementById('dropzone');
                const fileInput = document.getElementById('file-input');
                const loader = document.getElementById('loader');
                const loaderText = document.getElementById('loader-text');
                const dashboard = document.getElementById('dashboard');
                const uploadAnotherBtn = document.getElementById('upload-another-btn');
                const tagSearch = document.getElementById('tag-search');

                let parsedMetadata = null;
                let activeDirectoryName = null;

                // Drag and drop event listeners
                ['dragenter', 'dragover'].forEach(eventName => {
                    dropzone.addEventListener(eventName, (e) => {
                        e.preventDefault();
                        dropzone.classList.add('dragover');
                    }, false);
                });

                ['dragleave', 'drop'].forEach(eventName => {
                    dropzone.addEventListener(eventName, (e) => {
                        e.preventDefault();
                        dropzone.classList.remove('dragover');
                    }, false);
                });

                dropzone.addEventListener('drop', (e) => {
                    const dt = e.dataTransfer;
                    const files = dt.files;
                    if (files.length > 0) {
                        handleFile(files[0]);
                    }
                }, false);

                fileInput.addEventListener('change', (e) => {
                    if (fileInput.files.length > 0) {
                        handleFile(fileInput.files[0]);
                    }
                });

                uploadAnotherBtn.addEventListener('click', () => {
                    dashboard.style.display = 'none';
                    dropzone.style.display = 'block';
                    fileInput.value = '';
                    parsedMetadata = null;
                    activeDirectoryName = null;
                });

                tagSearch.addEventListener('input', () => {
                    renderTags();
                });

                function handleFile(file) {
                    // Show loader
                    dropzone.style.display = 'none';
                    loader.style.display = 'flex';
                    loaderText.innerText = 'Reading file: ' + file.name + '...';

                    // Update UI initial labels
                    document.getElementById('display-filename').innerText = file.name;
                    document.getElementById('display-filesize').innerText = formatBytes(file.size);

                    // Send raw file bytes to backend
                    fetch('/api/upload?filename=' + encodeURIComponent(file.name), {
                        method: 'POST',
                        body: file
                    })
                    .then(response => {
                        if (!response.ok) {
                            return response.json().then(err => { throw new Error(err.error || 'Server error'); });
                        }
                        return response.json();
                    })
                    .then(data => {
                        parsedMetadata = data;
                        loader.style.display = 'none';
                        dashboard.style.display = 'block';
                        
                        // Parse key info for summary cards
                        parseSummaryInfo(file);
                        
                        // Populate directories
                        populateDirectories();
                    })
                    .catch(error => {
                        loader.style.display = 'none';
                        dropzone.style.display = 'block';
                        alert('Error parsing file: ' + error.message);
                    });
                }

                function formatBytes(bytes, decimals = 2) {
                    if (bytes === 0) return '0 Bytes';
                    const k = 1024;
                    const dm = decimals < 0 ? 0 : decimals;
                    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
                    const i = Math.floor(Math.log(bytes) / Math.log(k));
                    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
                }

                function getTagValue(dirName, tagName) {
                    if (!parsedMetadata || !parsedMetadata.directories) return null;
                    const dir = parsedMetadata.directories.find(d => d.name === dirName);
                    if (!dir) return null;
                    const tag = dir.tags.find(t => t.name === tagName);
                    return tag ? tag.value : null;
                }

                function parseSummaryInfo(file) {
                    // Reset summary values
                    document.getElementById('summary-camera').innerText = '-';
                    document.getElementById('summary-software').innerText = '-';
                    document.getElementById('summary-date').innerText = '-';
                    document.getElementById('summary-resolution').innerText = '-';
                    document.getElementById('summary-filetype').innerText = file.type || 'Unknown Type';
                    document.getElementById('summary-gps').innerText = '-';
                    document.getElementById('summary-gps-link').innerHTML = 'No GPS coordinates';

                    if (!parsedMetadata || !parsedMetadata.directories) return;

                    // Camera Info
                    const make = getTagValue('Exif IFD0', 'Make');
                    const model = getTagValue('Exif IFD0', 'Model');
                    if (make || model) {
                        document.getElementById('summary-camera').innerText = (make || '') + ' ' + (model || '');
                    }
                    const software = getTagValue('Exif IFD0', 'Software');
                    if (software) {
                        document.getElementById('summary-software').innerText = 'Software: ' + software;
                    }

                    // Date
                    const dateDigitized = getTagValue('Exif SubIFD', 'Date/Time Digitized') || getTagValue('Exif SubIFD', 'Date/Time Original') || getTagValue('Exif IFD0', 'Date/Time');
                    if (dateDigitized) {
                        document.getElementById('summary-date').innerText = dateDigitized;
                    }

                    // Dimensions / Resolution
                    const width = getTagValue('Exif SubIFD', 'Exif Image Width') || getTagValue('PNG-IHDR', 'Image Width') || getTagValue('JPEG', 'Image Width') || getTagValue('GIF Header', 'Width') || getTagValue('BMP Header', 'Width');
                    const height = getTagValue('Exif SubIFD', 'Exif Image Height') || getTagValue('PNG-IHDR', 'Image Height') || getTagValue('JPEG', 'Image Height') || getTagValue('GIF Header', 'Height') || getTagValue('BMP Header', 'Height');
                    if (width && height) {
                        document.getElementById('summary-resolution').innerText = width + ' x ' + height + ' px';
                    }
                    
                    const fileType = getTagValue('File Type', 'Detected File Type Name');
                    if (fileType) {
                        document.getElementById('summary-filetype').innerText = fileType + ' File';
                    }

                    // GPS Info
                    const lat = getTagValue('GPS', 'GPS Latitude');
                    const lon = getTagValue('GPS', 'GPS Longitude');
                    const latRef = getTagValue('GPS', 'GPS Latitude Ref');
                    const lonRef = getTagValue('GPS', 'GPS Longitude Ref');
                    if (lat && lon) {
                        document.getElementById('summary-gps').innerText = lat + (latRef ? ' ' + latRef : '') + ', ' + lon + (lonRef ? ' ' + lonRef : '');
                        
                        // Parse numerical degrees for Google Maps link if possible
                        const latVal = parseGpsCoordinate(lat, latRef);
                        const lonVal = parseGpsCoordinate(lon, lonRef);
                        if (latVal !== null && lonVal !== null) {
                            document.getElementById('summary-gps-link').innerHTML = `
                                <a href="https://www.google.com/maps/search/?api=1&query=${latVal},${lonVal}" target="_blank" class="map-link">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
                                    Open in Google Maps
                                </a>
                            `;
                        }
                    }
                }

                function parseGpsCoordinate(coordStr, ref) {
                    try {
                        // Coordinates look like: 54° 59' 22.8" or similar
                        // Let's parse degrees, minutes, seconds using a regex
                        const regex = /(\\d+)\\s*[°º]\\s*(\\d+)\\s*['’′]\\s*([\\d.]+)\\s*["”″]/;
                        const match = coordStr.match(regex);
                        if (match) {
                            const deg = parseFloat(match[1]);
                            const min = parseFloat(match[2]);
                            const sec = parseFloat(match[3]);
                            let decimal = deg + (min / 60) + (sec / 3600);
                            if (ref === 'S' || ref === 'W') {
                                decimal = -decimal;
                            }
                            return decimal.toFixed(6);
                        }
                    } catch (e) {}
                    return null;
                }

                function populateDirectories() {
                    const dirList = document.getElementById('directory-list');
                    dirList.innerHTML = '';

                    if (!parsedMetadata || !parsedMetadata.directories || parsedMetadata.directories.length === 0) {
                        return;
                    }

                    // Sort directories: place 'File' and 'File Type' at the end, Exif at the beginning
                    const sortedDirs = [...parsedMetadata.directories].sort((a, b) => {
                        if (a.name.startsWith('Exif') && !b.name.startsWith('Exif')) return -1;
                        if (!a.name.startsWith('Exif') && b.name.startsWith('Exif')) return 1;
                        if (a.name === 'File' || a.name === 'File Type') return 1;
                        if (b.name === 'File' || b.name === 'File Type') return -1;
                        return a.name.localeCompare(b.name);
                    });

                    sortedDirs.forEach((dir, index) => {
                        const btn = document.createElement('button');
                        btn.className = 'directory-btn';
                        if (index === 0) {
                            btn.classList.add('active');
                            activeDirectoryName = dir.name;
                            document.getElementById('active-directory-title').innerText = dir.name;
                        }
                        btn.innerHTML = `
                            <span>${dir.name}</span>
                            <span class="tag-count">${dir.tags.length}</span>
                        `;
                        btn.addEventListener('click', () => {
                            document.querySelectorAll('.directory-btn').forEach(b => b.classList.remove('active'));
                            btn.classList.add('active');
                            activeDirectoryName = dir.name;
                            document.getElementById('active-directory-title').innerText = dir.name;
                            tagSearch.value = '';
                            renderTags();
                        });
                        dirList.appendChild(btn);
                    });

                    renderTags();
                }

                function renderTags() {
                    const tbody = document.getElementById('tag-table-body');
                    tbody.innerHTML = '';

                    if (!parsedMetadata || !activeDirectoryName) return;

                    const dir = parsedMetadata.directories.find(d => d.name === activeDirectoryName);
                    if (!dir) return;

                    const searchQuery = tagSearch.value.toLowerCase().trim();
                    const filteredTags = dir.tags.filter(tag => {
                        if (!searchQuery) return true;
                        return tag.name.toLowerCase().includes(searchQuery) || 
                               tag.value.toLowerCase().includes(searchQuery);
                    });

                    if (filteredTags.length === 0) {
                        tbody.innerHTML = `
                            <tr>
                                <td colspan="2" class="no-tags">No tags matching search query.</td>
                            </tr>
                        `;
                        return;
                    }

                    filteredTags.forEach(tag => {
                        const tr = document.createElement('tr');
                        
                        const nameTd = document.createElement('td');
                        nameTd.className = 'tag-name-col';
                        nameTd.innerText = tag.name;

                        const valTd = document.createElement('td');
                        valTd.className = 'tag-val-col';
                        valTd.innerText = tag.value;

                        tr.appendChild(nameTd);
                        tr.appendChild(valTd);
                        tbody.appendChild(tr);
                    });
                }
            </script>
        </body>
        </html>
        """;
    }
}
