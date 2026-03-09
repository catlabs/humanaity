import {ResolveFn} from '@angular/router';
import {CityService} from './city.service';
import {inject} from '@angular/core';
import {CityOutput} from '@api';

export const cityListResolver: ResolveFn<CityOutput[]> = (route, state) => {
  const service = inject(CityService);
  return service.getCities();
};

export const cityDetailsResolver: ResolveFn<CityOutput> = (route, state) => {
  const service = inject(CityService);
  const cityId = route.paramMap.get('id')!;
  return service.getCity(cityId);
};

export const myCitiesResolver: ResolveFn<CityOutput[]> = (route, state) => {
  const service = inject(CityService);
  return service.getMyCities();
};
