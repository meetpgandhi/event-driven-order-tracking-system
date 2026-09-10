import React from 'react';
import type { Stage } from '../types';
import { Check, AlertTriangle, RotateCcw, Truck, Box, PackageCheck, ShoppingBag } from 'lucide-react';

interface Props {
  currentStage: Stage;
}

const PRIMARY_FLOW: { stage: Stage; label: string; icon: any }[] = [
  { stage: 'ORDER_PLACED', label: 'Order Placed', icon: ShoppingBag },
  { stage: 'PICKING', label: 'Picking', icon: Box },
  { stage: 'PACKED', label: 'Packed', icon: PackageCheck },
  { stage: 'DISPATCHED', label: 'Dispatched', icon: Truck },
  { stage: 'OUT_FOR_DELIVERY', label: 'Out for Delivery', icon: Truck },
  { stage: 'DELIVERED', label: 'Delivered', icon: Check }
];

export const StageTimeline: React.FC<Props> = ({ currentStage }) => {
  const isFailed = currentStage === 'FAILED_ATTEMPT';
  const isReturned = currentStage === 'RETURNED';

  const getStageIndex = (stage: Stage): number => {
    switch (stage) {
      case 'ORDER_PLACED': return 0;
      case 'PICKING': return 1;
      case 'PACKED': return 2;
      case 'DISPATCHED': return 3;
      case 'OUT_FOR_DELIVERY': return 4;
      case 'DELIVERED': return 5;
      case 'FAILED_ATTEMPT': return 4;
      case 'RETURNED': return 5;
      default: return 0;
    }
  };

  const currentIndex = getStageIndex(currentStage);

  return (
    <div className="w-full py-6">
      {/* Primary Pipeline Progress */}
      <div className="relative flex items-center justify-between">
        {/* Progress Bar Background */}
        <div className="absolute top-5 left-6 right-6 h-1 bg-slate-200 -z-0" />
        
        {/* Active Progress Bar */}
        <div
          className="absolute top-5 left-6 h-1 bg-gradient-to-r from-blue-600 to-indigo-600 -z-0 transition-all duration-700"
          style={{
            width: `${Math.min(100, (currentIndex / (PRIMARY_FLOW.length - 1)) * 100)}%`
          }}
        />

        {PRIMARY_FLOW.map((item, index) => {
          const isCompleted = index < currentIndex;
          const isCurrent = index === currentIndex && !isFailed && !isReturned;
          const Icon = item.icon;

          return (
            <div key={item.stage} className="flex flex-col items-center relative z-10">
              <div
                className={`w-10 h-10 rounded-full flex items-center justify-center border-2 transition-all duration-300 ${
                  isCompleted
                    ? 'bg-blue-600 border-blue-600 text-white shadow-md shadow-blue-500/30'
                    : isCurrent
                    ? 'bg-white border-blue-600 text-blue-600 ring-4 ring-blue-100 shadow-lg'
                    : 'bg-white border-slate-300 text-slate-400'
                }`}
              >
                {isCompleted ? <Check className="w-5 h-5" /> : <Icon className="w-5 h-5" />}
              </div>
              <span
                className={`mt-2 text-xs font-semibold text-center max-w-[80px] ${
                  isCurrent ? 'text-blue-600' : isCompleted ? 'text-slate-800' : 'text-slate-400'
                }`}
              >
                {item.label}
              </span>
            </div>
          );
        })}
      </div>

      {/* Exception branch banner if applicable */}
      {isFailed && (
        <div className="mt-6 p-4 rounded-xl bg-amber-50 border border-amber-200 flex items-center space-x-3 text-amber-800">
          <AlertTriangle className="w-5 h-5 text-amber-600 flex-shrink-0" />
          <div className="text-sm">
            <span className="font-semibold">Delivery Attempt Exception:</span> Order is currently in{' '}
            <span className="font-bold">Failed Attempt</span> status. System auto-rescheduling is in progress for the next available slot.
          </div>
        </div>
      )}

      {isReturned && (
        <div className="mt-6 p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-center space-x-3 text-rose-800">
          <RotateCcw className="w-5 h-5 text-rose-600 flex-shrink-0" />
          <div className="text-sm">
            <span className="font-semibold">Order Returned:</span> Maximum delivery attempts reached. The parcel has been routed back to the fulfillment center.
          </div>
        </div>
      )}
    </div>
  );
};
