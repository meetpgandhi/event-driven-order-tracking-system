export type Stage =
  | 'ORDER_PLACED'
  | 'PICKING'
  | 'PACKED'
  | 'DISPATCHED'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'FAILED_ATTEMPT'
  | 'RETURNED';

export type ActorType = 'AGENT' | 'SYSTEM' | 'CARRIER' | 'ADMIN';

export interface Actor {
  actorId: string;
  actorType: ActorType;
}

export interface UserSession {
  accessToken: string;
  role: 'AGENT' | 'ADMIN';
  userId: number;
  email: string;
  name: string;
}

export interface AgentOrder {
  id: number;
  orderReference: string;
  customerName: string;
  customerPhone: string;
  customerEmail: string;
  currentStage: Stage;
  attemptCount: number;
  maxAttempts: number;
  nextDeliverySlot: string | null;
  availableNextStages: Stage[];
}

export interface EventTimelineItem {
  id: number;
  eventType: string;
  previousStage: Stage | null;
  newStage: Stage;
  newStageDisplayName: string;
  actorId: string;
  actorType: ActorType;
  reasonCode: string | null;
  timestamp: string;
}

export interface CustomerTrackingData {
  orderId: number;
  orderReference: string;
  customerName: string;
  currentStage: Stage;
  currentStageDisplayName: string;
  assignedAgentName: string | null;
  carrierId: string | null;
  attemptCount: number;
  maxAttempts: number;
  nextDeliverySlot: string | null;
  updatedAt: string;
  timeline: EventTimelineItem[];
}

export interface NotificationRule {
  id: number;
  eventType: string;
  channels: string[];
  templateBody: string;
  active: boolean;
  updatedAt: string;
}

export interface DashboardStats {
  totalOrders: number;
  ordersByStage: Record<string, number>;
  staleOrdersCount: number;
  todayEventsCount: number;
}

export interface AdminOrder {
  id: number;
  orderReference: string;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  currentStage: Stage;
  assignedAgentName: string | null;
  carrierId: string | null;
  attemptCount: number;
  maxAttempts: number;
  nextDeliverySlot: string | null;
  isStale: boolean;
  createdAt: string;
  updatedAt: string;
}
