import React, { useEffect, useState, useCallback } from 'react';
import axios from 'axios';
import ChartSection from './ChartSection';
import FestivalTrendChart from './FestivalTrendChart';

const App = () => {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(false);
  const [error, setError] = useState(null);
  const [range, setRange] = useState('all');
  const [sentiment, setSentiment] = useState('');
  const [location, setLocation] = useState('');
  const [theme, setTheme] = useState('');
  const [festival, setFestival] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [summary, setSummary] = useState({});
  const [activeTab, setActiveTab] = useState('articles');


  const loadArticles = useCallback(async () => {
    setLoading(true);
    try {
      const res = await axios.get('http://localhost:8080/api/insights/filter', {
        params: { range, sentiment, location, theme, page, size: 10 },
      });
      setArticles(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      console.error(err);
      setError('Failed to load filtered articles');
    } finally {
      setLoading(false);
    }
  }, [range, sentiment, location, theme, page]);

  const loadSummary = useCallback(async () => {
    setLoading(true);
    try {
      const res = await axios.get('http://localhost:8080/api/insights/summary', { params: { range } });
      setSummary(res.data);
    } catch (e) {
      console.error('Failed to load summary', e);
    } finally {
      setLoading(false);
    }
  }, [range]);

  const triggerFetch = async () => {
    setFetching(true);
    try {
      await axios.post('http://localhost:8080/api/articles/process');
      await loadArticles();
    } catch (err) {
      console.error(err);
      setError('Failed to trigger processing');
    } finally {
      setFetching(false);
    }
  };

  useEffect(() => { loadArticles(); }, [loadArticles]);
  useEffect(() => { loadSummary(); }, [loadSummary]);

  const getToneColor = (tone) => {
    switch (tone?.toLowerCase()) {
      case 'alarming': return '#e74c3c';
      case 'supportive': return '#2ecc71';
      case 'informative': return '#3498db';
      default: return '#7f8c8d';
    }
  };

  return (
    <div style={{ padding: '2rem', fontFamily: 'Segoe UI, sans-serif' }}>
      <h1 style={{ fontSize: '2rem', fontWeight: '700', color: '#2c3e50' }}>
        Animal Welfare Tracker <span role="img" aria-label="paw">🐾</span>
      </h1>
      <p>Latest articles on animal rescue, activism, and welfare in India.</p>

      <button
        onClick={triggerFetch}
        disabled={fetching}
        style={{
          padding: '8px 16px', backgroundColor: '#27ae60', color: 'white',
          border: 'none', borderRadius: '4px', fontWeight: 'bold', marginBottom: '20px'
        }}
      >
        {fetching ? 'Processing...' : 'Trigger Fetch from Feeds'}
      </button>

      {/* TABS */}
      <div style={{ marginBottom: '20px', display: 'flex', gap: '10px' }}>
        <button
          onClick={() => setActiveTab('articles')}
          style={{
            padding: '8px 16px',
            backgroundColor: activeTab === 'articles' ? '#3498db' : '#ecf0f1',
            color: activeTab === 'articles' ? 'white' : '#2c3e50',
            border: 'none',
            borderRadius: '6px',
            fontWeight: 'bold',
            cursor: 'pointer'
          }}
        >
          📄 Articles
        </button>
        <button
          onClick={() => setActiveTab('charts')}
          style={{
            padding: '8px 16px',
            backgroundColor: activeTab === 'charts' ? '#3498db' : '#ecf0f1',
            color: activeTab === 'charts' ? 'white' : '#2c3e50',
            border: 'none',
            borderRadius: '6px',
            fontWeight: 'bold',
            cursor: 'pointer'
          }}
        >
          📊 Charts
        </button>
      </div>

      {/* CONDITIONAL TAB CONTENT */}
      {activeTab === 'articles' && (
        <>
          {/* Filters */}
          <div style={{
            marginBottom: '20px', padding: '16px', backgroundColor: '#f9f9f9',
            borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
            display: 'flex', flexWrap: 'wrap', gap: '12px', alignItems: 'center'
          }}>
             <div>
                 <label style={{ fontWeight: 'bold' }}>Time Range:</label><br />
                 <select value={range} onChange={(e) => setRange(e.target.value)} style={{ padding: '4px', width: '150px' }}>
                    <option value="week">Last Week</option>
                    <option value="month">Last Month</option>
                    <option value="year">Last Year</option>
                    <option value="all">All</option>
                 </select>
             </div>
             <div>
                <label style={{ fontWeight: 'bold' }}>Sentiment:</label><br />
                <select value={sentiment} onChange={(e) => setSentiment(e.target.value)} style={{ padding: '4px', width: '150px' }}>
                    <option value="">All</option>
                    <option value="POSITIVE">Positive</option>
                    <option value="NEGATIVE">Negative</option>
                </select>
             </div>
             <div>
               <label style={{ fontWeight: 'bold' }}>Festival:</label><br />
               <input
                 value={festival}
                 onChange={(e) => setFestival(e.target.value)}
                 placeholder="e.g. Diwali, Holi"
                 style={{ padding: '4px', width: '150px' }}
               />
             </div>
             <div>
                <label style={{ fontWeight: 'bold' }}>Theme:</label><br />
                <select value={theme} onChange={(e) => setTheme(e.target.value)} style={{ padding: '4px', width: '150px' }}>
                    <option value="">All</option>
                    <option value="Animal Rescue">Animal Rescue</option>
                    <option value="Cruelty">Cruelty</option>
                    <option value="Adoption">Adoption</option>
                    <option value="Wildlife">Wildlife</option>
                    <option value="Activism">Activism</option>
                    <option value="Festivals">Festivals</option>
                    <option value="Awareness">Awareness</option>
                    <option value="Education">Education</option>
                    <option value="Stray Dogs">Stray Dogs</option>
                    <option value="Other">Other</option>
                </select>
             </div>
             <div style={{ alignSelf: 'flex-end' }}>
                <button onClick={() => { setPage(0); loadArticles(); }} style={{ padding: '6px 12px', backgroundColor: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }} >
                    Apply Filters
                </button>
             </div>
          </div>

          {loading && <p>Loading articles...</p>}
          {error && <p style={{ color: 'red' }}>{error}</p>}

          <ul style={{ listStyle: 'none', padding: 0 }}>
            {articles.map((article, idx) => (
              <li key={idx} style={{
                border: '1px solid #ddd', borderRadius: '8px',
                padding: '16px', marginBottom: '16px', backgroundColor: '#fafafa',
                boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
              }}>
                <div style={{ color: '#666', fontSize: '13px' }}>
                  🗓️ Published: {new Date(article.publishedDate).toLocaleDateString('en-IN')}
                </div>

                <a href={article.url} target="_blank" rel="noopener noreferrer"
                  style={{ fontSize: '18px', fontWeight: '600', color: '#2c3e50', textDecoration: 'none' }}>
                  {article.title}
                </a>

                {article.festivalLinked && (
                  <span style={{
                    marginLeft: '10px', backgroundColor: '#f39c12', color: 'white',
                    fontSize: '12px', fontWeight: 'bold', padding: '2px 6px', borderRadius: '4px'
                  }}>
                    🎉 Festival
                  </span>
                )}

                <div dangerouslySetInnerHTML={{ __html: article.summary }}
                  style={{ marginTop: '8px', color: '#555', fontSize: '14px' }} />

                <div style={{ marginTop: '8px', fontStyle: 'italic', color: '#333' }}>
                  {article.themes?.join(', ')} —
                  <span style={{ fontWeight: 'bold', color: article.sentiment === 'POSITIVE' ? 'green' : 'red' }}>
                    {article.sentiment}
                  </span>
                </div>

                {article.tone && (
                  <div style={{ marginTop: '6px', color: getToneColor(article.tone), fontWeight: 'bold' }}>
                    🧠 Tone: {article.tone}
                  </div>
                )}

                {article.authorities?.length > 0 && (
                  <div style={{ marginTop: '6px', color: '#2c3e50' }}>
                    🏛️ <strong>Authorities:</strong> {article.authorities.join(', ')}
                  </div>
                )}

                {article.location && (
                  <div style={{ marginTop: '6px', color: '#9b59b6' }}>
                    📍 <strong>Location:</strong> {article.location}
                  </div>
                )}
              </li>
            ))}
          </ul>

          {/* Pagination */}
          <div style={{ marginTop: '1rem' }}>
            <button onClick={() => setPage(Math.max(0, page - 1))} disabled={page === 0} style={{ marginRight: '8px' }}>
              Prev
            </button>
            Page {page + 1}
            <button onClick={() => setPage(page + 1)} disabled={page + 1 >= totalPages} style={{ marginLeft: '8px' }}>
              Next
            </button>
          </div>
        </>
      )}

      {activeTab === 'charts' && Object.keys(summary).length > 0 && (
        <>
          <ChartSection summary={summary} />
          <FestivalTrendChart />
        </>
      )}
    </div>
  );
};

export default App;

