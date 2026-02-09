package ru.mipt.software2.provider;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import ru.mipt.software2.api.currency.CurrencyServiceGrpc;
import ru.mipt.software2.api.currency.GetRateRequest;
import ru.mipt.software2.api.currency.GetRateResponse;

@Service
public class CurrencyServiceImpl extends CurrencyServiceGrpc.CurrencyServiceImplBase {

    private final RateService rateService;

    public CurrencyServiceImpl(RateService rateService) {
        this.rateService = rateService;
    }

    @Override
    public void getRate(GetRateRequest request, StreamObserver<GetRateResponse> responseObserver) {
        double rate = rateService.getCurrentRate();

        GetRateResponse response = GetRateResponse.newBuilder()
                .setUsdrub(rate)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

