import { AgentUiEffectOutput } from './agentUiEffectOutput';
import { SimulationCommandReferencedEntitiesOutput } from './simulationCommandReferencedEntitiesOutput';

/**
 * Humanaity API
 *
 * NOTE: Manually added for milestone 2 until OpenAPI regeneration.
 */
export interface SimulationCommandOutput {
    ok?: boolean;
    commandType?: string;
    message?: string;
    mutated?: boolean;
    referencedEntities?: SimulationCommandReferencedEntitiesOutput;
    uiEffects?: Array<AgentUiEffectOutput>;
}
