export * from './authController.service';
import { AuthControllerService } from './authController.service';
export * from './cities.service';
import { CitiesService } from './cities.service';
export * from './humans.service';
import { HumansService } from './humans.service';
export * from './simulations.service';
import { SimulationsService } from './simulations.service';
export const APIS = [AuthControllerService, CitiesService, HumansService, SimulationsService];
