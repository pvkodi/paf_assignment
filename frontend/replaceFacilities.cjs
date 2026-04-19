const fs = require('fs');

const files = [
  'c:/Users/ravab/Documents/GitHub/paf_assignment/frontend/src/features/facilities/TimetablePreviewPage.jsx',
  'c:/Users/ravab/Documents/GitHub/paf_assignment/frontend/src/features/facilities/FacilityDetailsPage.jsx',
  'c:/Users/ravab/Documents/GitHub/paf_assignment/frontend/src/features/facilities/FacilityManagementDashboard.jsx',
  'c:/Users/ravab/Documents/GitHub/paf_assignment/frontend/src/features/facilities/FacilitySearch.jsx'
];

files.forEach(file => {
  if (!fs.existsSync(file)) return;
  
  let content = fs.readFileSync(file, 'utf8');

  // Container updates
  content = content.replace(/rounded-lg/g, 'rounded-2xl');
  content = content.replace(/rounded-md/g, 'rounded-xl');
  content = content.replace(/shadow-md/g, 'shadow-sm');
  
  // Slate and Gray colors to Craftboard Hex
  content = content.replace(/text-slate-900/g, 'text-[#0f172a]');
  content = content.replace(/text-gray-900/g, 'text-[#0f172a]');
  
  content = content.replace(/text-slate-800/g, 'text-[#1e293b]');
  content = content.replace(/text-gray-800/g, 'text-[#1e293b]');
  
  content = content.replace(/text-slate-700/g, 'text-[#334155]');
  content = content.replace(/text-gray-700/g, 'text-[#334155]');
  
  content = content.replace(/text-slate-600/g, 'text-[#475569]');
  content = content.replace(/text-gray-600/g, 'text-[#475569]');
  
  content = content.replace(/text-slate-500/g, 'text-[#64748b]');
  content = content.replace(/text-gray-500/g, 'text-[#64748b]');
  
  content = content.replace(/text-slate-400/g, 'text-[#94a3b8]');
  content = content.replace(/text-gray-400/g, 'text-[#94a3b8]');
  
  content = content.replace(/text-slate-300/g, 'text-[#cbd5e1]');
  content = content.replace(/text-gray-300/g, 'text-[#cbd5e1]');
  
  content = content.replace(/border-slate-300/g, 'border-[#e2e8f0]');
  content = content.replace(/border-gray-300/g, 'border-[#e2e8f0]');
  
  content = content.replace(/border-slate-200/g, 'border-[#e2e8f0]');
  content = content.replace(/border-gray-200/g, 'border-[#e2e8f0]');
  
  content = content.replace(/border-slate-100/g, 'border-[#f1f5f9]');
  content = content.replace(/border-gray-100/g, 'border-[#f1f5f9]');
  
  content = content.replace(/bg-slate-100/g, 'bg-[#f1f5f9]');
  content = content.replace(/bg-gray-100/g, 'bg-[#f1f5f9]');
  
  content = content.replace(/bg-slate-50/g, 'bg-[#f8fafc]');
  content = content.replace(/bg-gray-50/g, 'bg-[#f8fafc]');

  content = content.replace(/bg-slate-900/g, 'bg-[#0f172a]');
  content = content.replace(/bg-gray-900/g, 'bg-[#0f172a]');

  // Primary buttons (blue to neutral black)
  content = content.replace(/bg-blue-600/g, 'bg-[#0f172a]');
  content = content.replace(/hover:bg-blue-700/g, 'hover:bg-[#1e293b]');
  content = content.replace(/from-blue-600/g, 'from-[#0f172a]');
  content = content.replace(/to-blue-700/g, 'to-[#0f172a]');
  content = content.replace(/hover:from-blue-700/g, 'hover:from-[#1e293b]');
  content = content.replace(/hover:to-blue-800/g, 'hover:to-[#1e293b]');

  // Secondary elements updates
  content = content.replace(/text-xs font-semibold text-\\[#64748b\\] uppercase tracking-wide/g, 'text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider');
  content = content.replace(/text-xs font-semibold text-gray-500 mb-1 tracking-wide uppercase/g, 'text-[10px] font-bold text-[#94a3b8] mb-2 tracking-wider uppercase');
  content = content.replace(/text-xs font-semibold uppercase tracking-wider text-\\[#94a3b8\\]/g, 'text-[10px] font-bold uppercase tracking-widest text-[#94a3b8]');
  
  // Adjust heading sizing
  content = content.replace(/text-3xl font-bold text-\\[#0f172a\\]/g, 'text-2xl font-bold tracking-tight text-[#0f172a]');
  content = content.replace(/text-2xl font-bold/g, 'text-2xl font-bold tracking-tight');

  fs.writeFileSync(file, content);
});
