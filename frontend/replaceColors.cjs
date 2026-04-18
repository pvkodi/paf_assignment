const fs = require('fs');
const file = 'c:/Users/ravab/Documents/GitHub/paf_assignment/frontend/src/features/analytics/UtilizationDashboard.jsx';
let content = fs.readFileSync(file, 'utf8');

content = content.replace(/text-gray-900/g, 'text-[#0f172a]');
content = content.replace(/text-gray-800/g, 'text-[#1e293b]');
content = content.replace(/text-gray-700/g, 'text-[#334155]');
content = content.replace(/text-gray-600/g, 'text-[#475569]');
content = content.replace(/text-gray-500/g, 'text-[#64748b]');
content = content.replace(/text-gray-400/g, 'text-[#94a3b8]');
content = content.replace(/text-gray-300/g, 'text-[#cbd5e1]');

content = content.replace(/bg-gray-50/g, 'bg-[#f8fafc]');
content = content.replace(/bg-gray-100/g, 'bg-[#f1f5f9]');
content = content.replace(/bg-gray-200/g, 'bg-[#e2e8f0]');
content = content.replace(/bg-gray-900/g, 'bg-[#0f172a]');

content = content.replace(/border-gray-100/g, 'border-[#e2e8f0]');
content = content.replace(/border-gray-200/g, 'border-[#e2e8f0]');

content = content.replace(/fill-gray-900/g, 'fill-[#0f172a]');
content = content.replace(/fill-gray-400/g, 'fill-[#94a3b8]');
content = content.replace(/fill-gray-300/g, 'fill-[#cbd5e1]');

content = content.replace(/stroke="#f1f5f9"/g, 'stroke="#f8fafc"');

fs.writeFileSync(file, content);
