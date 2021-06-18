package com.algamoneyapi.resource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algamoneyapi.event.RecursoCriadoEvent;
import com.algamoneyapi.exceptionhandler.AlgamoneyExceptionHandler.Erro;
import com.algamoneyapi.model.Lancamento;
import com.algamoneyapi.model.Pessoa;
import com.algamoneyapi.repository.LancamentoRepository;
import com.algamoneyapi.repository.PessoaRepository;
import com.algamoneyapi.repository.filter.LancamentoFilter;
import com.algamoneyapi.service.LancamentoService;
import com.algamoneyapi.service.exception.PessoaInativaException;

@RestController
@RequestMapping("/lancamentos")
public class LancamentoResource {
	
	@Autowired
	private LancamentoRepository lancamentoRepository;
	
	@Autowired
	private PessoaRepository pessoaRepository;
	
	@Autowired
	private ApplicationEventPublisher publisher;
	
	@Autowired
	private LancamentoService lancamentoService;
	
	@Autowired
	private MessageSource messageSource;
	
	@GetMapping
	public ResponseEntity<Object> pesquisar(LancamentoFilter lancamentoFilter, Pageable pageable){
		Page<Lancamento> lancamentos = lancamentoRepository.filtrar(lancamentoFilter, pageable);
		if(lancamentos.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(lancamentos);
	}
	
	@GetMapping("/{codigo}")
	public ResponseEntity<?> listarLancamentoPorId(@PathVariable Long codigo){
		Optional<Lancamento> lancamento = lancamentoRepository.findById(codigo);
		if(lancamento.isPresent()) {
			return ResponseEntity.status(HttpStatus.OK).body(lancamento.get());
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("O lançamento com CÓDIGO "+codigo+" não foi encontrado ou não existe");
	}
	
	@PostMapping
	public ResponseEntity<?> adicionarLancamento(@Valid @RequestBody Lancamento lancamento, HttpServletResponse response){
		
		Optional<Pessoa> pessoa = pessoaRepository.findById(lancamento.getPessoa().getCodigo());
		if(pessoa.isPresent()) {
			lancamento.setPessoa(pessoa.get());
		}else {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("A pessoa não existe ou não foi encontrada");
		}
		
		Lancamento lancamentoSalvo = lancamentoService.salvar(lancamento);
		
		publisher.publishEvent(new RecursoCriadoEvent(this, response, lancamentoSalvo.getCodigo()));
		
		return ResponseEntity.status(HttpStatus.CREATED).body(lancamentoSalvo);
	}
	
	@DeleteMapping("/{codigo}")
	public ResponseEntity<?> deletarLancamento(@PathVariable Long codigo){
		lancamentoRepository.deleteById(codigo);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
	
	@ExceptionHandler( { PessoaInativaException.class } )
	public ResponseEntity<Object> handlePessoaInativaException( PessoaInativaException ex){
		String mensagemUsuario = messageSource.getMessage("pessoa.inativa", null, LocaleContextHolder.getLocale());
		String mensagemDesenvolvedor = ex.toString();
		List<Erro> erros = Arrays.asList(new Erro(mensagemUsuario, mensagemDesenvolvedor));
		return ResponseEntity.badRequest().body(erros);
	}
	
	
}
