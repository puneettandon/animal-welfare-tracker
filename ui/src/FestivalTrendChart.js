import React, { useEffect, useState } from 'react';
import axios from 'axios';
import {
  BarChart, Bar, XAxis, YAxis,
  CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';

const rangeOptions = {
  '1month': 'Last 1 Month',
  '3months': 'Last 3 Months',
  '6months': 'Last 6 Months',
  '1year': 'Last 1 Year',
  'all': 'All Time'
};

const FestivalTrendChart = () => {
  const [data, setData] = useState([]);
  const [range, setRange] = useState('3months');
  const [tempRange, setTempRange] = useState('3months');
  const [rangeLabel, setRangeLabel] = useState('');

  useEffect(() => {
    fetchFestivalTrends(range);
  }, [range]);

  const fetchFestivalTrends = async (selectedRange) => {
    try {
      const res = await axios.get(`/api/insights/festival-summary?range=${selectedRange}`);
      console.log('Festival summary response:', res.data);
      setData(res.data.data || []);
      setRangeLabel(res.data.rangeLabel || '');
    } catch (err) {
      console.error('Error loading festival summary', err);
    }
  };

  return (
    <div style={{ padding: '1rem', marginTop: '2rem' }}>
      <h3 style={{ marginBottom: '0.5rem' }}>🎉 Festival Trend Summary</h3>

      <div style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
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

      {rangeLabel && (
        <p style={{ fontStyle: 'italic', color: '#555', marginBottom: '1rem' }}>
          {rangeLabel}
        </p>
      )}

      {data.length === 0 ? (
        <p>No data found for selected range.</p>
      ) : (
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="event" angle={-30} textAnchor="end" interval={0} />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar dataKey="positive" fill="#2ecc71" name="Positive" />
            <Bar dataKey="negative" fill="#e74c3c" name="Negative" />
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  );
};

export default FestivalTrendChart;
