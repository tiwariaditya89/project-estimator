import React, { useState } from "react";
import axios from "axios";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeRaw from "rehype-raw";
import rehypeHighlight from "rehype-highlight";
import "highlight.js/styles/github.css";

export default function App() {
  const [file, setFile] = useState(null);
  const [result, setResult] = useState("");
  const [feedback, setFeedback] = useState("");
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);

  // Upload PDF and generate initial estimate
  const handleUpload = async () => {
    if (!file) {
      alert("Please select a PDF file first!");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    setLoading(true);
    try {
      const res = await axios.post(
        "http://localhost:8080/api/estimate/upload",
        formData,
        { headers: { "Content-Type": "multipart/form-data" } }
      );
      setResult(res.data);
    } catch (err) {
      alert("Error uploading PDF: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  // Send feedback to regenerate estimate
  const handleRegenerate = async () => {
    if (!feedback.trim()) return;

    setLoading(true);
    try {
      const res = await axios.post(
        "http://localhost:8080/api/estimate/feedback",
        {
          scopeText: result,
          feedback,
        }
      );
      setResult(res.data);
      setFeedback("");
    } catch (err) {
      alert("Error sending feedback: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  // Download result as PDF or DOCX
  const handleDownload = async (type) => {
    if (!result) {
      alert("No estimation available to download!");
      return;
    }

    setDownloading(true);
    try {
      const endpoint =
        type === "pdf"
          ? "http://localhost:8080/api/download/pdf"
          : "http://localhost:8080/api/download/docx";

      const res = await axios.post(
        endpoint,
        { scopeText: result },
        { responseType: "blob" }
      );

      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute(
        "download",
        type === "pdf" ? "estimation.pdf" : "estimation.docx"
      );
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert("Error downloading file: " + err.message);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div style={styles.container}>
      <h1 style={styles.title}>Your Scope.Your Estimate.Instantly.</h1>
      <p style={styles.subtitle}>
        Upload a project PDF and get instant estimation.
      </p>

      <div style={styles.uploadContainer}>
        <input
          type='file'
          accept='application/pdf'
          onChange={(e) => setFile(e.target.files[0])}
          style={styles.fileInput}
        />
        <button onClick={handleUpload} style={styles.button} disabled={loading}>
          {loading ? "Processing..." : "Generate Estimate"}
        </button>
      </div>

      {result && (
        <div style={styles.resultContainer}>
          <h3>Generated Estimate:</h3>
          <div style={styles.markdownWrapper}>
            <ReactMarkdown
              remarkPlugins={[remarkGfm]}
              rehypePlugins={[rehypeHighlight, rehypeRaw]}
            >
              {result}
            </ReactMarkdown>
          </div>

          <div style={styles.downloadContainer}>
            <button
              style={styles.buttonSecondary}
              onClick={() => handleDownload("pdf")}
              disabled={downloading}
            >
              {downloading ? "Downloading..." : "Download as PDF"}
            </button>
            <button
              style={styles.buttonSecondary}
              onClick={() => handleDownload("docx")}
              disabled={downloading}
            >
              {downloading ? "Downloading..." : "Download as DOCX"}
            </button>
          </div>

          <div style={styles.feedbackContainer}>
            <textarea
              placeholder='Enter feedback to refine the estimate...'
              value={feedback}
              onChange={(e) => setFeedback(e.target.value)}
              style={styles.textarea}
            />
            <button
              style={styles.buttonAlt}
              onClick={handleRegenerate}
              disabled={!feedback.trim() || loading}
            >
              {loading ? "Regenerating..." : "Regenerate with Feedback"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// --- Styles ---
const styles = {
  container: {
    maxWidth: "800px",
    margin: "40px auto",
    padding: "20px",
    background: "#f9fafb",
    borderRadius: "12px",
    boxShadow: "0 6px 15px rgba(0,0,0,0.1)",
    borderTop: "4px solid #2563eb",
    textAlign: "center",
    fontFamily: "'Inter', sans-serif",
    transition: "box-shadow 0.3s ease",
    cursor: "default",
  },
  title: {
    fontSize: "2.2rem",
    fontWeight: "700",
    marginBottom: "10px",
    color: "#111827",
  },
  subtitle: {
    color: "#2563eb", // match container accent
    fontWeight: "500",
    marginBottom: "25px",
  },

  uploadContainer: {
    display: "flex",
    justifyContent: "center",
    gap: "10px",
    marginBottom: "25px",
  },
  fileInput: {
    border: "1px solid #d1d5db",
    borderRadius: "6px",
    padding: "8px",
    width: "60%",
  },
  button: {
    background: "#2563eb",
    color: "#fff",
    border: "none",
    padding: "10px 20px",
    borderRadius: "8px",
    cursor: "pointer",
    fontWeight: "500",
  },
  buttonAlt: {
    background: "#10b981",
    color: "#fff",
    border: "none",
    padding: "10px 18px",
    borderRadius: "8px",
    cursor: "pointer",
    fontWeight: "500",
  },
  buttonSecondary: {
    background: "#0ea5e9",
    color: "#fff",
    border: "none",
    padding: "10px 16px",
    borderRadius: "10px", // a bit more rounded
    cursor: "pointer",
    fontWeight: "500",
    transition: "background 0.2s ease, transform 0.2s ease",
  },
  buttonSecondaryHover: {
    background: "#0284c7",
    transform: "translateY(-2px)",
  },
  downloadContainer: {
    display: "flex",
    justifyContent: "center",
    gap: "10px",
    marginTop: "10px",
  },
  resultContainer: {
    marginTop: "25px",
    background: "#fff",
    padding: "22px",
    borderRadius: "10px",
    border: "1px solid #e5e7eb", // subtle border
    boxShadow: "0 4px 12px rgba(0,0,0,0.08)",
    textAlign: "left",
  },
  markdownWrapper: {
    maxHeight: "600px",
    overflowY: "auto",
    padding: "12px",
    borderRadius: "6px",
    backgroundColor: "#f3f4f6",
    fontSize: "14px",
    lineHeight: "1.5",
  },
  feedbackContainer: { marginTop: "20px" },
  textarea: {
    width: "100%",
    minHeight: "100px",
    borderRadius: "8px",
    border: "1px solid #d1d5db",
    padding: "10px",
    marginBottom: "10px",
    resize: "vertical",
  },
};
