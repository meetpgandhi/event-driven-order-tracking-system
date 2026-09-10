import { Client } from '@stomp/stompjs';

export function createWebSocketClient(onEventReceived: (event: any) => void): Client {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const brokerURL = `${protocol}//${window.location.host}/ws/dashboard`;

  const client = new Client({
    brokerURL,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      console.log('Connected to STOMP WebSocket broker');
      client.subscribe('/topic/dashboard-events', (message) => {
        try {
          const payload = JSON.parse(message.body);
          onEventReceived(payload);
        } catch (e) {
          console.error('Error parsing STOMP message', e);
        }
      });
    },
    onStompError: (frame) => {
      console.error('STOMP error', frame);
    }
  });

  return client;
}
