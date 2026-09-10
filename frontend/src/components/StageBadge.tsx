import type { Stage } from '../types';

interface Props {
  stage: Stage;
  size?: 'sm' | 'md' | 'lg';
}

const STAGE_CONFIG: Record<Stage, { label: string; bg: string; text: string; border: string }> = {
  ORDER_PLACED: { label: 'Order Placed', bg: 'bg-blue-50', text: 'text-blue-700', border: 'border-blue-200' },
  PICKING: { label: 'Picking', bg: 'bg-indigo-50', text: 'text-indigo-700', border: 'border-indigo-200' },
  PACKED: { label: 'Packed', bg: 'bg-purple-50', text: 'text-purple-700', border: 'border-purple-200' },
  DISPATCHED: { label: 'Dispatched', bg: 'bg-amber-50', text: 'text-amber-700', border: 'border-amber-200' },
  OUT_FOR_DELIVERY: { label: 'Out for Delivery', bg: 'bg-sky-50', text: 'text-sky-700', border: 'border-sky-200' },
  DELIVERED: { label: 'Delivered', bg: 'bg-emerald-50', text: 'text-emerald-700', border: 'border-emerald-200' },
  FAILED_ATTEMPT: { label: 'Failed Attempt', bg: 'bg-rose-50', text: 'text-rose-700', border: 'border-rose-200' },
  RETURNED: { label: 'Returned', bg: 'bg-slate-100', text: 'text-slate-700', border: 'border-slate-300' }
};

export const StageBadge: React.FC<Props> = ({ stage, size = 'md' }) => {
  const config = STAGE_CONFIG[stage] || { label: stage, bg: 'bg-gray-50', text: 'text-gray-700', border: 'border-gray-200' };

  const sizeClasses = {
    sm: 'text-xs px-2 py-0.5',
    md: 'text-xs px-2.5 py-1 font-medium',
    lg: 'text-sm px-3.5 py-1.5 font-semibold'
  }[size];

  return (
    <span className={`inline-flex items-center rounded-full border ${config.bg} ${config.text} ${config.border} ${sizeClasses}`}>
      <span className="w-1.5 h-1.5 rounded-full mr-1.5 bg-current opacity-70"></span>
      {config.label}
    </span>
  );
};
