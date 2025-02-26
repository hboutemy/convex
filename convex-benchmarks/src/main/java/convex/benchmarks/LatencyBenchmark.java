package  convex.benchmarks;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;

import convex.api.Convex;
import convex.core.Coin;
import convex.core.Result;
import convex.core.crypto.AKeyPair;
import convex.core.data.Address;
import convex.core.lang.ops.Constant;
import convex.core.transactions.Invoke;
import convex.peer.API;
import convex.peer.Server;

/**
 * Benchmark for full round-trip latencies
 * 
 * Note: these are for a single client executing transactions sequentially, and are
 * therefore not useful for measuring overall network throughput (which would have many
 * clients submitting transactions in parallel).
 */
public class LatencyBenchmark {
	
	static Address HERO=null;
	static Address VILLAIN=null;
	static final AKeyPair[] KPS=new AKeyPair[] {AKeyPair.generate(),AKeyPair.generate()};

	static Server server;
	static Convex client;
	static Convex client2;
	static Convex peer;
	static {
		List<Server> servers=API.launchLocalPeers(Benchmarks.PEER_KEYPAIRS, Benchmarks.STATE, null, null);
		server=servers.get(0);
		try {
			Thread.sleep(1000);
			peer=Convex.connect(server,server.getPeerController(),server.getKeyPair());
			HERO=peer.createAccountSync(KPS[0].getAccountKey());
			VILLAIN=peer.createAccountSync(KPS[1].getAccountKey());
			peer.transfer(HERO, Coin.EMERALD);
			peer.transfer(VILLAIN, Coin.EMERALD);
			
			client=Convex.connect(server.getHostAddress(), HERO,KPS[0]);
			client2=Convex.connect(server.getHostAddress(), VILLAIN,KPS[1]);
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Benchmark
	public void roundTripTransaction() throws TimeoutException, IOException {
		client.transactSync(Invoke.create(Benchmarks.HERO,-1, Constant.of(1L)));
		// System.out.println(server.getBroadcastCount());
	}

	@Benchmark
	public void roundTripTwoTransactions() throws TimeoutException, IOException, InterruptedException, ExecutionException {
		Future<Result> r1=client.transact(Invoke.create(HERO,-1, Constant.of(1L)));
		Future<Result> r2=client2.transact(Invoke.create(VILLAIN,-1, Constant.of(1L)));
		r1.get(1000,TimeUnit.MILLISECONDS);
		r2.get(1000,TimeUnit.MILLISECONDS);
	}

	@Benchmark
	public void roundTrip10Transactions() throws TimeoutException, IOException, InterruptedException, ExecutionException {
		doTransactions(10);
	}

	@Benchmark
	public void roundTrip50Transactions() throws TimeoutException, IOException, InterruptedException, ExecutionException {
		doTransactions(50);
	}

	@Benchmark
	public void roundTrip1000Transactions() throws TimeoutException, IOException, InterruptedException, ExecutionException {
		doTransactions(1000);
	}

	@SuppressWarnings("unchecked")
	private void doTransactions(int n) throws IOException, InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Result>[] rs=new CompletableFuture[n];
		for (int i=0; i<n; i++) {
			CompletableFuture<Result> f=client.transact(Invoke.create(HERO,-1, Constant.of(i)));
			rs[i]=f;
		}
		CompletableFuture.allOf(rs).get(1000,TimeUnit.MILLISECONDS);
		Result r0=rs[0].get();
		if (r0.isError()) {
			throw new Error("Transaction failed: "+r0);
		}
	}

	@Benchmark
	public void roundTripQuery() throws TimeoutException, IOException, InterruptedException, ExecutionException {
		client.querySync(Constant.of(1L));
	}


	public static void main(String[] args) throws Exception {
		Options opt = Benchmarks.createOptions(LatencyBenchmark.class);
		new Runner(opt).run();
	}
}
