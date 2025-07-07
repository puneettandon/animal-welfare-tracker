import React, { useEffect, useState } from 'react';
import axios from 'axios';
import {
  PieChart, Pie, Cell,
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer
} from 'recharts';

const COLORS = ['#2ecc71', '#e74c3c'];

const rangeOptions = {
  '1month': 'Last 1 Month',
  '3months': 'Last 3 Months',
  '6months': 'Last 6 Months',
  '1year': 'Last 1 Year',
  'all': 'All Time'
};

const ChartSection = () => {
  const [summary, setSummary] = useState({});
  const [range, setRange] = useState('3months');
  const [tempRange, setTempRange] = useState('3months');

  useEffect(() => {
    fetchSummary(range);
  }, [range]);

  const fetchSummary = async (selectedRange) => {
    try {
      const res = await axios.get(`/api/insights/summary?range=${selectedRange}`);
      console.log("Fetched summary:", res.data);
      setSummary(res.data);
    } catch (err) {
      console.error("Error loading chart summary:", err);
    }
  };

  const sentimentData = [
    { name: 'Positive', value: summary.positive || 0 },
    { name: 'Negative', value: summary.negative || 0 },
  ];

  const themeData = Object.entries(summary.themeCounts || {}).map(([k, v]) => ({ name: k, count: v }));
  const locationData = Object.entries(summary.locationCounts || {}).map(([k, v]) => ({ name: k, count: v }));

  const containerStyle = {
    minWidth: '300px',
    width: '100%',
    height: 300,
    padding: '1rem',
    boxSizing: 'border-box'
  };

  return (
    <div style={{ marginTop: '2rem' }}>
      <h2 style={{ marginBottom: '0.5rem' }}>📊 Insights Summary</h2>

      {/* Dropdown and Apply */}
      <div style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <label htmlFor="range" style={{ fontWeight: 500 }}>📅 Time Range:</label>
        <select
          id="range"
          value={tempRange}
          onChange={(e) => setTempRange(e.target.value)}
          style={{
            padding: '0.4rem 0.6rem',
            fontSize: '1rem',
            borderRadius: '5px',
            border: '1px solid #ccc',
            outline: 'none'
          }}
        >
          {Object.entries(rangeOptions).map(([key, label]) => (
            <option key={key} value={key}>{label}</option>
          ))}
        </select>

        <button
          onClick={() => setRange(tempRange)}
          disabled={tempRange === range}
          style={{
            padding: '0.4rem 0.8rem',
            backgroundColor: '#2ecc71',
            color: '#fff',
            border: 'none',
            borderRadius: '5px',
            cursor: tempRange === range ? 'not-allowed' : 'pointer'
          }}
        >
          Apply
        </button>
      </div>

      {/* Optional subtitle */}
     {rangeOptions[range] && (
       <p style={{ fontStyle: 'italic', color: '#555', marginBottom: '1.5rem' }}>
         {rangeOptions[range]}
       </p>
     )}


      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'space-around',
        gap: '2rem'
      }}>
        <div style={containerStyle}>
          <h4>Sentiment</h4>
          <div style={{ width: 300, height: 250 }}>
            {sentimentData.every(d => d.value === 0) ? (
              <p>No sentiment data available</p>
            ) : (
              <PieChart width={300} height={250}>
                <Pie
                  data={sentimentData}
                  dataKey="value"
                  nameKey="name"
                  outerRadius={80}
                  label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                >
                  {sentimentData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            )}
          </div>
        </div>

        <div style={containerStyle}>
          <h4>Themes</h4>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={themeData}>
              <XAxis dataKey="name" tick={{ fontSize: 10 }} interval={0} angle={-30} textAnchor="end" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill="#3498db" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div style={containerStyle}>
          <h4>Locations</h4>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={locationData}>
              <XAxis dataKey="name" tick={{ fontSize: 10 }} interval={0} angle={-30} textAnchor="end" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill="#9b59b6" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
};

export default ChartSection;
