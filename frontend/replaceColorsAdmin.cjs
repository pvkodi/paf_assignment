const fs = require('fs');
const file = 'c:/Users/ravab/Documents/GitHub/paf_assignment/frontend/src/features/admin/AdminUserManagementPanel.jsx';
let content = fs.readFileSync(file, 'utf8');

// Container updates
content = content.replace(/rounded-lg/g, 'rounded-2xl');
content = content.replace(/rounded-md/g, 'rounded-xl');
content = content.replace(/shadow-md/g, 'shadow-sm');

// Slate colors to Craftboard Hex
content = content.replace(/slate-900/g, '[#0f172a]');
content = content.replace(/slate-800/g, '[#1e293b]');
content = content.replace(/slate-700/g, '[#334155]');
content = content.replace(/slate-600/g, '[#64748b]');
content = content.replace(/slate-500/g, '[#94a3b8]');
content = content.replace(/slate-400/g, '[#cbd5e1]');
content = content.replace(/slate-300/g, '[#cbd5e1]');
content = content.replace(/slate-200/g, '[#e2e8f0]');
content = content.replace(/slate-100/g, '[#f1f5f9]');
content = content.replace(/slate-50/g, '[#f8fafc]');

// Table headers
content = content.replace(/text-xs font-semibold text-\\[#64748b\\] uppercase tracking-wide/g, 'text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider');

// Error/Success backgrounds
content = content.replace(/bg-red-50/g, 'bg-[#fef2f2]');
content = content.replace(/border-red-200/g, 'border-[#fca5a5]');
content = content.replace(/text-red-900/g, 'text-[#991b1b]');
content = content.replace(/text-red-700/g, 'text-[#b91c1c]');
content = content.replace(/text-red-600/g, 'text-[#dc2626]');
content = content.replace(/border-red-600/g, 'border-[#ef4444]');
content = content.replace(/bg-green-50/g, 'bg-[#f0fdf4]');
content = content.replace(/text-green-700/g, 'text-[#15803d]');
content = content.replace(/text-green-600/g, 'text-[#16a34a]');
content = content.replace(/border-green-600/g, 'border-[#22c55e]');

// Adjust heading sizing
content = content.replace(/text-3xl font-bold/g, 'text-2xl font-bold tracking-tight');

fs.writeFileSync(file, content);
