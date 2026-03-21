/** Matches GET /api/simulations/assistant/commands */
export type SimulationAssistantCommandDescriptor = {
  commandType: string;
  canonicalText: string;
  label: string;
  description: string;
};

export type SimulationAssistantMetric = {
  label: string;
  value: string;
};

export type SimulationAssistantItem = {
  title: string;
  subtitle: string | null;
  body: string | null;
  chips: string[];
};

export type SimulationAssistantBlock = {
  type: string;
  title: string;
  subtitle: string | null;
  metrics: SimulationAssistantMetric[];
  items: SimulationAssistantItem[];
  emptyState: string | null;
};

export type SimulationAssistantResponse = {
  ok: boolean;
  commandType: string;
  text: string | null;
  blocks: SimulationAssistantBlock[];
};
